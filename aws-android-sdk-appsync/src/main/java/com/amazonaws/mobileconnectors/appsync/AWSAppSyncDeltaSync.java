/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.retry.RetryInterceptor;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

class AWSAppSyncDeltaSync {
    //Constant for Logging
    private static final String TAG = AWSAppSyncDeltaSync.class.getSimpleName();

    private Context mContext;

    //Map that keeps a list of all the DeltaSync objects
    private static Map<Long, AWSAppSyncDeltaSync> deltaSyncObjects = new HashMap<Long, AWSAppSyncDeltaSync>();

    //Constants and attributes to manage subscription mode
    private static final String QUEUING_MODE = "QUEUING_MODE";
    private static final String PROCESSING_MODE  = "PROCESSING_MODE";
    private String mode = null;
    private Object processingLock = new Object();

    //Flags and locks to manage network events
    private static Boolean networkUp = true;
    private static Object networkLock = new Object();

    //Flags and locks to manage app lifecycle events
    private static Boolean appInForeground = true;
    private static Object foregroundLock = new Object();

    //Components of the Delta Sync Object
    private Query baseQuery;
    private GraphQLCall.Callback<Query.Data> baseQueryCallback = null;
    private Subscription subscription;
    private AppSyncSubscriptionCall.Callback subscriptionCallback;
    private Query deltaQuery = null;
    private long lastRunTimeInMilliSeconds = 0;

    //Default value of 24 hours
    private long baseRefreshIntervalInSeconds = 24 * 60 * 60;
    private GraphQLCall.Callback<Query.Data> deltaQueryCallback = null;
    private Long id;

    AppSyncSubscriptionCall deltaSyncSubscriptionWatcher = null;
    private ArrayDeque<Response> messageQueue = new ArrayDeque<Response>();

    //Appsync client used to perform the deltasync operations
    private AWSAppSyncClient awsAppSyncClient;

    //Delta sync operation status
    private boolean deltaSyncOperationFailed = false;

    //Delta sync status - Active or cancelled?
    private boolean cancelled = false;

    //Persistence mechanism
    private static AWSAppSyncDeltaSyncSqlHelper awsAppSyncDeltaSyncSqlHelper = null;
    private AWSAppSyncDeltaSyncDBOperations dbHelper = null;
    private Object initializationLock = new Object();
    private boolean recordCreatedOrFound = false;

    //Scheduled Sync
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture nextScheduledRun = null;

    //Exponential backoff
    int retryAttempt = 0;
    private ScheduledFuture nextRetryAttempt = null;

    private AppSyncSubscriptionCall.Callback scb = null;

    //Construtor
     <D extends Query.Data, T, V extends Query.Variables> AWSAppSyncDeltaSync(
            @Nonnull Query<D, T, V> baseQuery,
            AWSAppSyncClient awsAppSyncClient,
            Context context) {
        this.mContext = context;
        this.baseQuery = baseQuery;
        this.awsAppSyncClient = awsAppSyncClient;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    //Initialize the Delta Sync persistence mechanism
    private void initializeIfRequired() {
        synchronized(initializationLock) {
            Log.d(TAG, "In initialize method");
            if (awsAppSyncDeltaSyncSqlHelper == null) {
                //Setup up the local store
                Log.d(TAG, "Initializing the database");
                awsAppSyncDeltaSyncSqlHelper = new AWSAppSyncDeltaSyncSqlHelper(mContext,
                        awsAppSyncClient.deltaSyncSqlStoreName);
            }

            if (dbHelper == null ) {
                dbHelper = new AWSAppSyncDeltaSyncDBOperations(awsAppSyncDeltaSyncSqlHelper);
            }

            if (!recordCreatedOrFound) {
                AWSAppSyncDeltaSyncDBOperations.DeltaSyncRecord record;
                if ((record = dbHelper.getRecordByKey(getKey())) == null ) {
                    this.id = dbHelper.createRecord(getKey(), lastRunTimeInMilliSeconds);
                }
                else {
                    this.id = record.id;
                    this.lastRunTimeInMilliSeconds = record.lastRunTimeInMilliSeconds;
                }
                deltaSyncObjects.put(id,this);
                recordCreatedOrFound = true;
            }

        }
    }

    void setBaseQuery(Query baseQuery) {
        this.baseQuery = baseQuery;
    }

    void setBaseQueryCallback(GraphQLCall.Callback<Query.Data> callback) {
        this.baseQueryCallback = callback;
    }

    void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    void setSubscriptionCallback( AppSyncSubscriptionCall.Callback subscriptionCallback) {
        this.subscriptionCallback = subscriptionCallback;
    }


    void setDeltaQuery(Query deltaQuery) {
        this.deltaQuery = deltaQuery;
    }

    void setDeltaQueryCallback(GraphQLCall.Callback<Query.Data> callback) {
        this.deltaQueryCallback = callback;
    }

    void setBaseRefreshIntervalInSeconds(long baseRefreshIntervalInSeconds) {
        this.baseRefreshIntervalInSeconds = baseRefreshIntervalInSeconds;
    }

    private String getKey( ) {
        return  "" + baseQuery + subscription + deltaQuery;
    }

    Long execute(final boolean forceFetch) {
        //Initialize the Delta Sync Machinery if required.
        initializeIfRequired();

        if (cancelled) {
            Log.v(TAG, "Delta Sync: Cancelled. Quitting Delta Sync process for id [" + id + "]");
            return id;
        }

        this.deltaSyncOperationFailed = false;
        new Thread( new Runnable() {
            @Override
            public void run() {
                final CountDownLatch baseQueryCountdownLatch = new CountDownLatch(1);
                Log.v(TAG, "Delta Sync: Starting Sync process");

                //Run base query from the cache
                runBaseQuery(AppSyncResponseFetchers.CACHE_ONLY);
                if (deltaSyncOperationFailed ) {
                    scheduleRetry();
                    return;
                }

                //Subscribe
                if (subscription != null ) {
                    mode = QUEUING_MODE;
                    subscribe();
                    if (deltaSyncOperationFailed) {
                        scheduleRetry();
                        return;
                    }
                }

                //Check if we need to execute the DeltaQuery or BaseQuery from the network.
                boolean executeBaseQuery = true;
                if (!forceFetch && deltaQuery != null) {
                    //Check if
                    long deltaInSeconds = (System.currentTimeMillis() - (lastRunTimeInMilliSeconds - 2000)) / 1000;
                    Log.v(TAG, "Delta Sync: Time since last sync [" + deltaInSeconds + "] seconds");
                    if (deltaInSeconds < baseRefreshIntervalInSeconds) {
                        Log.v(TAG, "The last baseQuery from NETWORK was executed less than [" + baseRefreshIntervalInSeconds + "] seconds ago. Will run DeltaQuery from network");
                        executeBaseQuery = false;
                    }
                    else {
                        Log.v(TAG, "The last baseQuery from NETWORK run was before [" + baseRefreshIntervalInSeconds + "] seconds. Will run BaseQuery from network");
                    }
                } else {
                    Log.v(TAG, "Will run BaseQuery from network");
                }

                if (executeBaseQuery) {
                    runBaseQuery(AppSyncResponseFetchers.NETWORK_ONLY);
                } else {
                    runDeltaQuery();
                }

                if (deltaSyncOperationFailed) {
                    scheduleRetry();
                    return;
                }

                //Propagate all messages received so far and put subscription in processing mode
                synchronized (processingLock) {
                    Log.v(TAG, "Delta Sync: Delta query completed. Will propagate any queued messages on subscription");
                    Response r;
                    while ((r = messageQueue.poll()) != null) {
                        if (subscriptionCallback != null ) {
                            Log.v(TAG, "Delta Sync: Propagating queued message on subscription");
                            subscriptionCallback.onResponse(r);
                        }
                    }
                    Log.d(TAG, "Delta Sync: All queued messages propagated. Flipping mode to PROCESSING");
                    mode = PROCESSING_MODE;
                }

                retryAttempt = 0;
            }
        }).start();

        return id;
    }

    void cancel() {
         Log.i(TAG, "Delta Sync: Cancelling Delta Sync operation [" + id + "]");
        cancelled = true;
        if (deltaSyncSubscriptionWatcher != null ) {
            deltaSyncSubscriptionWatcher.cancel();
        }
        if (nextRetryAttempt != null ) {
            nextRetryAttempt.cancel(true);
            nextRetryAttempt = null;
        }
        if (nextScheduledRun != null ) {
            nextScheduledRun.cancel(true);
            nextScheduledRun = null;
        }
        deltaSyncObjects.remove(id);
    }

    static void cancel(Long id) {
         AWSAppSyncDeltaSync deltaSync = deltaSyncObjects.get(id);
         if (deltaSync != null ) {
             deltaSync.cancel();
         }
    }

    /*
        This method should be called when the device/app transitions from being offline to online
     */
    static void handleNetworkUpEvent() {

        //Guard against duplicate notifications of network up. The transition should result in
        //delta sync being run once.
        synchronized (networkLock) {
            if ( !networkUp ) {
                networkUp = true;
                for (Map.Entry<Long, AWSAppSyncDeltaSync> ds : deltaSyncObjects.entrySet()) {
                    Log.d(TAG, "Delta Sync: Network Up detected. Running DeltaSync for ds object [" + ds.getKey() + "]");
                    ds.getValue().cancelRetry();
                    ds.getValue().execute(false);
                }
            }
        }
    }

    /*
        This method should be called when the device/app transistions from being online to offline
     */
    static void handleNetworkDownEvent() {
        synchronized (networkLock) {
            if (networkUp) {
                Log.d(TAG, "Delta Sync: Network Down detected.");
                networkUp = false;
            }
        }
    }

    /*
        This method should be called when the device/app transitions from being the background to foreground
     */
    static void handleAppForeground() {

        //Guard against duplicate notifications of app transitioning to foreground . The transition should result in
        //delta sync being run once.
        synchronized (foregroundLock) {
            if ( !appInForeground ) {
                appInForeground = true;
                synchronized (networkLock) {
                    if (networkUp ) {
                        for (Map.Entry<Long, AWSAppSyncDeltaSync> ds : deltaSyncObjects.entrySet()) {
                            Log.d(TAG, "Delta Sync: Foreground transition detected. Running DeltaSync for ds object [" + ds.getKey() + "]");
                            ds.getValue().cancelRetry();
                            ds.getValue().execute(false);
                        }
                    }
                }
            }
        }
    }

    /*
        This method should be called when the device/app transitions from the foreground to the background
     */
    static void handleAppBackground() {
        //Guard against multiple notifications of app transitioning to background.
        synchronized (foregroundLock) {
            if (appInForeground) {
                Log.d(TAG, "Delta Sync: Background transition detected.");
                appInForeground = false;
            }
        }
    }

    /*
       Schedule a Sync based on the periodicRefreshInterval. When this call is made,
       it will cancel any previously scheduled run and will setup a new one to be executed
       after periodicRefreshIntervalInSeconds
     */
    private void scheduleFutureSync(long offset) {
        //Calculate the offset
        if (baseRefreshIntervalInSeconds <= 0 ) {
            Log.i(TAG, "Delta Sync: baseRefreshIntervalInSeconds value is [" + baseRefreshIntervalInSeconds + "]. Will not schedule future Deltasync");
            return;
        }
        if (nextScheduledRun != null ) {
            nextScheduledRun.cancel(false);
        }

        long runAtTime = (offset - System.currentTimeMillis())/1000 + baseRefreshIntervalInSeconds;
        Log.v(TAG, "Delta Sync: Scheduling next run of the DeltaSync [" + runAtTime + "] seconds from now");

        final WeakReference<AWSAppSyncDeltaSync> thisObjectRef = new WeakReference<AWSAppSyncDeltaSync>(this);
        nextScheduledRun = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                if (thisObjectRef.get() != null ) {
                    thisObjectRef.get().execute(true);
                }
            }
        }, runAtTime, TimeUnit.SECONDS);
    }

    private void scheduleRetry() {
        long runAtTime = RetryInterceptor.calculateBackoff(retryAttempt);
        Log.v(TAG, "Delta Sync: Scheduling retry of the DeltaSync [" + runAtTime + "] milliseconds from now");

        final WeakReference<AWSAppSyncDeltaSync> thisObjectRef = new WeakReference<AWSAppSyncDeltaSync>(this);
        nextRetryAttempt = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                if (thisObjectRef.get() != null ) {
                    thisObjectRef.get().execute(false);
                }
            }
        }, runAtTime, TimeUnit.MILLISECONDS);

        //increment RetryAttempt
        retryAttempt++;
    }

    void cancelRetry() {
        // cancel any future retry attempts.
        // This is to cover the case of a new delta sync being triggered by an event (network event, foreground event or periodic)
        // while the previous attempt is still kicking around in the retry sequence.

        if (nextRetryAttempt != null ) {
            nextRetryAttempt.cancel(false);
            nextRetryAttempt = null;
        }
        retryAttempt = 0;
    }

    Query adjust(Query deltaQuery) {
        long lastRunTime = lastRunTimeInMilliSeconds/1000;
        Log.v(TAG, "Delta Sync: Attempting to set lastSync in DeltaQuery to [" + lastRunTime + "]");
        Query aq = deltaQuery;
        try {
            Operation.Variables v = aq.variables();
            Field f = v.getClass().getDeclaredField("lastSync");
            f.setAccessible(true);
            f.set(v,lastRunTime);
            Log.v(TAG, "Delta Sync: set lastSync in DeltaQuery to [" + lastRunTime + "]");

        } catch (NoSuchFieldException e) {
            Log.v(TAG, "Delta Sync: field 'lastSync' not present in query. Skipping adjustment");
        } catch (IllegalAccessException e) {
            Log.v(TAG, "Delta Sync: Unable to override value in for 'lastSync'. Skipping adjustment");
        }
        return aq;
    }

    void runBaseQuery(final ResponseFetcher fetchPolicy) {

        final CountDownLatch baseQueryCountdownLatch = new CountDownLatch(1);
        final long baseQueryInitiationTime = System.currentTimeMillis();

        //Setup an internal callback for the hand off
        GraphQLCall.Callback<Query.Data> cb = new GraphQLCall.Callback<Query.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<Query.Data> response) {
                Log.v(TAG, "Delta Sync: Base query response received");

                if (AppSyncResponseFetchers.NETWORK_ONLY.equals(fetchPolicy)) {

                    //Setup the scheduler for a future Sync
                    scheduleFutureSync(baseQueryInitiationTime);

                    //Update lastRunTime
                    lastRunTimeInMilliSeconds = baseQueryInitiationTime;
                    dbHelper.updateLastRunTime(id,lastRunTimeInMilliSeconds);
                    Log.v(TAG, "Delta Sync: Updating lastRunTime to [" + lastRunTimeInMilliSeconds + "]");
                }
                dbHelper.updateLastRunTime(id,lastRunTimeInMilliSeconds);
                if (baseQueryCallback != null ) {
                    baseQueryCallback.onResponse(response);
                }
                Log.v(TAG, "Delta Sync: Base query response propagated");
                baseQueryCountdownLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                Log.e(TAG, "Delta Query: BaseQuery failed with [" + e.getLocalizedMessage() +"]");
                e.printStackTrace();
                deltaSyncOperationFailed = true;
                if (baseQueryCallback != null ) {
                    baseQueryCallback.onFailure(e);
                }
                baseQueryCountdownLatch.countDown();
            }
        };

        if (AppSyncResponseFetchers.CACHE_ONLY.equals(fetchPolicy)) {
            Log.v(TAG, "Delta Sync: executing base query from cache");
        }
        else {
            Log.v(TAG, "Delta Sync: executing base query from network");
        }

        //Execute the base query.
        awsAppSyncClient.query(baseQuery)
                .responseFetcher(fetchPolicy)
                .enqueue(cb);

        try {
            baseQueryCountdownLatch.await();
        }
        catch (InterruptedException iex) {
            Log.e(TAG, "Delta Sync: Base Query wait failed with [" + iex + "]");
            deltaSyncOperationFailed = true;
        }

    }

    void subscribe() {
        Log.v(TAG, "Delta Sync: Subscription was passed in. Setting it up");
        //Setup an internal callback for the handoff
        Log.v(TAG, "Delta Sync: Setting mode to QUEUING");

        if (scb == null ) {
            scb = new AppSyncSubscriptionCall.Callback() {
                @Override
                public void onResponse(@Nonnull Response response) {
                    Log.d(TAG, "Got a Message. Current mode is " + mode);
                    synchronized (processingLock) {
                        if (mode == QUEUING_MODE) {
                            Log.v(TAG, "Delta Sync: Message received while in QUEUING mode. Adding to queue");
                            messageQueue.add(response);
                        } else {
                            Log.v(TAG, "Delta Sync: Message received while in PROCESSING mode.");
                            lastRunTimeInMilliSeconds = System.currentTimeMillis();
                            dbHelper.updateLastRunTime(id, lastRunTimeInMilliSeconds);
                            Log.v(TAG, "Delta Sync: Updating lastRunTime to [" + lastRunTimeInMilliSeconds + "]");
                            if (subscriptionCallback != null) {
                                Log.v(TAG, "Delta Sync: Propagating received message");
                                subscriptionCallback.onResponse(response);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "Delta Sync: onFailure executed with exception: [" + e.getLocalizedMessage() + "]");
                    //Propagate
                    if (subscriptionCallback != null) {
                        Log.v(TAG, "Delta Sync: Propagating onFailure");
                        subscriptionCallback.onFailure(e);
                    }
                }

                @Override
                public void onCreated() {
                    Log.e(TAG, "Delta Sync: onCreated executed for subscription");
                }

                @Override
                public void onCompleted() {
                    Log.e(TAG, "Delta Sync: onCompleted executed for subscription");

                }
            };
        }

        Log.d(TAG,"Delta Sync: Setting up Delta Sync Subscription Watcher");
        deltaSyncSubscriptionWatcher = awsAppSyncClient.subscribe(subscription);
        deltaSyncSubscriptionWatcher.execute(scb);
    }

    void runDeltaQuery() {
        Log.v(TAG, "Delta Sync: executing Delta query");
        final CountDownLatch deltaQueryCountdownLatch = new CountDownLatch(1);

        final long deltaQueryInitiationTime = System.currentTimeMillis();
        //Setup an internal callback for the hand off
        GraphQLCall.Callback<Query.Data> dcb = new GraphQLCall.Callback<Query.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<Query.Data> response) {
                Log.v(TAG, "Delta Sync: Received response for Delta Query.");
                lastRunTimeInMilliSeconds = deltaQueryInitiationTime;
                dbHelper.updateLastRunTime(id,lastRunTimeInMilliSeconds);
                Log.v(TAG, "Delta Sync: Updated lastRunTime to  [" + lastRunTimeInMilliSeconds + "]");

                if (deltaQueryCallback != null ) {
                    Log.v(TAG, "Delta Sync: Propagating Delta query response.");
                    deltaQueryCallback.onResponse(response);
                }
                deltaQueryCountdownLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                Log.e(TAG, "Delta Sync: onFailure executed for Delta Query with [" + e.getLocalizedMessage() +"]");
                deltaSyncOperationFailed = true;
                if (deltaQueryCallback != null ) {
                    Log.v(TAG, "Delta Sync: Propagating onFailure");
                    deltaQueryCallback.onFailure(e);
                }
                deltaQueryCountdownLatch.countDown();
            }
        };

        awsAppSyncClient.query(adjust(deltaQuery))
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(dcb);

        try {
            deltaQueryCountdownLatch.await();
        }
        catch(InterruptedException iex) {
            Log.e(TAG, "Delta Sync: Delta Query wait failed with [" + iex + "]");
            deltaSyncOperationFailed = true;
        }
    }

}
