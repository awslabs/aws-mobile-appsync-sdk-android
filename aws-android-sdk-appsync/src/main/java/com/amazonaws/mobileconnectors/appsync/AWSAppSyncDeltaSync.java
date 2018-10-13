/**
 * Copyright 2018-2018 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.util.Log;

import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
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

class AWSAppSyncDeltaSync  {
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
    private long periodicRefreshIntervalInSeconds = 0;
    private long deltaSyncWindowInSeconds = 0;
    private GraphQLCall.Callback<Query.Data> deltaQueryCallback = null;
    private Long id;

    AppSyncSubscriptionCall deltaSyncSubscriptionWatcher = null;
    private ArrayDeque<Response> messageQueue = new ArrayDeque<Response>();

    //Appsync client used to perform the deltasync operations
    private AWSAppSyncClient awsAppSyncClient;

    //Delta sync operation status
    private boolean failed = false;

    //Delta sync status - Active or cancelled?
    private boolean cancelled = false;

    //Persistence mechanism
    private static AWSAppSyncDeltaSyncSqlHelper awsAppSyncDeltaSyncSqlHelper = null;
    private AWSAppSyncDeltaSyncDBOperations dbHelper = null;
    private Object initializationLock = new Object();
    private boolean recordCreatedOrFound = false;

    //Scheduled Sync
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture nextRun = null;

    private AppSyncSubscriptionCall.Callback scb;

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
            if (awsAppSyncDeltaSyncSqlHelper == null ) {
                //Setup up the local store
                Log.d(TAG, "Initializing the database");
                awsAppSyncDeltaSyncSqlHelper = new AWSAppSyncDeltaSyncSqlHelper(mContext);
            }

            if( dbHelper == null ) {
                dbHelper = new AWSAppSyncDeltaSyncDBOperations(awsAppSyncDeltaSyncSqlHelper);
            }

            if (!recordCreatedOrFound) {

                AWSAppSyncDeltaSyncDBOperations.DeltaSyncRecord record;
                if ((record = dbHelper.getRecordByKey(getKey())) == null ) {
                    this.id = dbHelper.createRecord(getKey(), lastRunTimeInMilliSeconds, deltaSyncWindowInSeconds,periodicRefreshIntervalInSeconds);
                }
                else {
                    this.id = record.id;
                    this.lastRunTimeInMilliSeconds = record.lastRunTimeInMilliSeconds;
                    this.periodicRefreshIntervalInSeconds = record.periodicRefreshIntervalInSeconds;
                    this.deltaSyncWindowInSeconds = record.deltaSyncWindowInSeconds;
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

    void setPeriodicRefreshIntervalInSeconds(long periodicRefreshIntervalInSeconds) {
        this.periodicRefreshIntervalInSeconds = periodicRefreshIntervalInSeconds;
    }

    void setDeltaSyncWindowInSeconds(long deltaSyncWindowInSeconds) {
        this.deltaSyncWindowInSeconds = deltaSyncWindowInSeconds;
    }

    private String getKey( ) {
        return  "" + baseQuery + subscription + deltaQuery;
    }

    AWSAppSyncDeltaSync execute(final boolean forceFetch) {

        //Initialize the Delta Sync Machinery if required.
        initializeIfRequired();

        //Setup the scheduler for a future Sync based on the value set for periodicRefreshInterval
        scheduleFutureSync();
        this.failed = false;
        //TODO: Consider creating a thread pool for these threads.
        new Thread( new Runnable() {
            @Override
            public void run() {
                final CountDownLatch baseQueryCountdownLatch = new CountDownLatch(1);
                Log.d(TAG, "Delta Sync: Starting Sync process");

                final long baseQueryInitiationTime = System.currentTimeMillis();
                //Determine if it is a cache fetch or a network fetch
                ResponseFetcher f = AppSyncResponseFetchers.CACHE_ONLY;
                if (forceFetch) {
                    f = AppSyncResponseFetchers.NETWORK_ONLY;
                    lastRunTimeInMilliSeconds = baseQueryInitiationTime;
                    Log.d(TAG, "Delta Sync: Setting basequery cache mode to NETWORK_ONLY");
                }
                else {
                    //Check if the call needs to be from the cache or from the network
                    long deltaInSeconds = (System.currentTimeMillis() - (lastRunTimeInMilliSeconds - 2000)) / 1000;
                    Log.v(TAG, "Delta Sync: Time since last sync [" + deltaInSeconds + "] seconds");
                    if (deltaInSeconds > deltaSyncWindowInSeconds) {
                        f = AppSyncResponseFetchers.NETWORK_ONLY;
                        Log.v(TAG, "Delta Sync: Setting basequery cache mode to NETWORK_ONLY");
                    } else {
                        Log.v(TAG, "Delta Sync: Setting basequery cache mode to CACHE_ONLY");
                    }
                }

                //Set the fetchPolicy into a final variable so that the callback has access to it.
                final ResponseFetcher fetchPolicy = f;
                //Setup an internal callback for the hand off
                GraphQLCall.Callback<Query.Data> cb = new GraphQLCall.Callback<Query.Data>() {
                    @Override
                    public void onResponse(@Nonnull final Response<Query.Data> response) {
                        Log.v(TAG, "Delta Sync: Base query response received");

                        if (AppSyncResponseFetchers.NETWORK_ONLY.equals(fetchPolicy)) {
                            lastRunTimeInMilliSeconds = System.currentTimeMillis();
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
                        failed = true;
                        if (baseQueryCallback != null ) {
                            baseQueryCallback.onFailure(e);
                        }
                        baseQueryCountdownLatch.countDown();
                    }
                };


                Log.v(TAG, "Delta Sync: executing base query");
                //Execute the base query.
                awsAppSyncClient.query(baseQuery)
                        .responseFetcher(fetchPolicy)
                        .enqueue(cb);

                try {
                    baseQueryCountdownLatch.await();
                }
                catch (InterruptedException iex) {
                    Log.e(TAG, "Delta Sync: Base Query wait failed with [" + iex + "]");
                    failed = true;
                }

                if (failed ) {
                    return;
                }


                //Subscribe
                if (subscription != null ) {
                    Log.v(TAG, "Delta Sync: Subscription was passed in. Setting it up");
                    //Setup an internal callback for the handoff
                    mode = QUEUING_MODE;
                    Log.v(TAG, "Delta Sync: Setting mode to QUEUING");

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
                                    dbHelper.updateLastRunTime(id,lastRunTimeInMilliSeconds);
                                    Log.v(TAG, "Delta Sync: Updating lastRunTime to [" + lastRunTimeInMilliSeconds + "]");
                                    if (subscriptionCallback != null ) {
                                        Log.v(TAG, "Delta Sync: Propagating received message");
                                        subscriptionCallback.onResponse(response);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(@Nonnull ApolloException e) {

                            /*
                            if ("Subscription Infrastructure: Failed to create client for subscription".equals(e.getLocalizedMessage())) {
                                //Got an error indicating the the subscription creation failed.
                                //This could be due to a server error or local network drop.
                                //The local network drop will be handled by the NetworkListener
                                //The following code will retry the connection only if the network was up.
                                synchronized (networkLock) {
                                    if (networkUp && !cancelled) {
                                        Log.d(TAG, "Delta Sync [" + id + "]: Received Unable to create subscription error. Will try again.");
                                        deltaSyncSubscriptionWatcher = awsAppSyncClient.subscribe(subscription);
                                        deltaSyncSubscriptionWatcher.execute(this);

                                    }
                                }
                                return;
                            }
                            */
                            Log.e(TAG, "Delta Sync: onFailure executed with exception: [" + e.getLocalizedMessage() + "]");
                            //Propagate
                            if (subscriptionCallback != null ) {
                                Log.v(TAG, "Delta Sync: Propagating onFailure");
                                subscriptionCallback.onFailure(e);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            //Resubscribe if there was an error
                            if (!cancelled) {
                                synchronized (networkLock) {
                                    if (networkUp) {
                                        Log.v(TAG, "Delta Sync [" + id + "]: Received disconnect for subscription. Network is up. Will reconnect.");
                                        deltaSyncSubscriptionWatcher = awsAppSyncClient.subscribe(subscription);
                                        deltaSyncSubscriptionWatcher.execute(this);
                                    }
                                    else {
                                        Log.d(TAG, "Delta Sync [" + id + "]: Received disconnect for subscription. Network is down. Will defer to the NetworkListener to setup the connection.");
                                    }
                                }
                            }
                            //Otherwise propagate
                            else {
                                Log.d(TAG, "Delta Sync [" + id + "]: Received disconnect for subscription. Expected. Will propagate.");
                                subscriptionCallback.onCompleted();
                            }
                        }
                    };

                    Log.d(TAG,"Delta Sync: Setting up Delta Sync Subscription Watcher");
                    deltaSyncSubscriptionWatcher = awsAppSyncClient.subscribe(subscription);
                    deltaSyncSubscriptionWatcher.execute(scb);
                }

                //Execute the delta query
                if ( deltaQuery != null ) {
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
                            failed = true;
                            if (deltaQueryCallback != null ) {
                                Log.v(TAG, "Delta Sync: Propagating onFailure");
                                deltaQueryCallback.onFailure(e);
                            }
                            deltaQueryCountdownLatch.countDown();
                        }
                    };

                    Query adjustedDeltaQuery = adjust(deltaQuery);

                    awsAppSyncClient.query(deltaQuery)
                            .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                            .enqueue(dcb);

                    try {
                        deltaQueryCountdownLatch.await();
                    }
                    catch(InterruptedException iex) {
                        Log.e(TAG, "Delta Sync: Delta Query wait failed with [" + iex + "]");
                    }
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
            }
        }).start();

        return this;
    }

    void cancel() {
        cancelled = true;
        if (deltaSyncSubscriptionWatcher != null ) {
            deltaSyncSubscriptionWatcher.cancel();
        }
        deltaSyncObjects.remove(id);
        dbHelper.deleteRecord(id);
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
                    Log.d(TAG, "Network Up detected. Running DeltaSync for ds object [" + ds.getKey() + "]");
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
                Log.d(TAG, "Network Down detected.");
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
                for (Map.Entry<Long, AWSAppSyncDeltaSync> ds : deltaSyncObjects.entrySet()) {
                    Log.d(TAG, "Foreground transition detected. Running DeltaSync for ds object [" + ds.getKey() + "]");
                    ds.getValue().execute(false);
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
                Log.d(TAG, "Background transition detected.");
                appInForeground = false;
            }
        }
    }

    /*
       Schedule a Sync based on the periodicRefreshInterval. When this call is made,
       it will cancel any previously scheduled run and will setup a new one to be executed
       after periodicRefreshIntervalInSeconds
     */
    private void scheduleFutureSync() {
        if (periodicRefreshIntervalInSeconds <= 0 ) {
            Log.i(TAG, "Invalid value for periodicRefreshIntervalInSeconds[" + periodicRefreshIntervalInSeconds + "]. Must be greater than 0");
            return;
        }
        if (nextRun != null ) {
            nextRun.cancel(false);
        }
        final WeakReference<AWSAppSyncDeltaSync> thisObjectRef = new WeakReference<AWSAppSyncDeltaSync>(this);
        nextRun = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                if (thisObjectRef.get() != null ) {
                    thisObjectRef.get().execute(true);
                }
            }
        }, periodicRefreshIntervalInSeconds, TimeUnit.SECONDS);
    }



    Query adjust(Query deltaQuery) {
        Log.v(TAG, "Delta Sync: Attempting to set lastSync in DeltaQuery to [" + lastRunTimeInMilliSeconds + "]");
        Query aq = deltaQuery;
        try {
            Operation.Variables v = aq.variables();
            Field f = v.getClass().getDeclaredField("lastSync");
            f.setAccessible(true);
            f.set(v,""+ lastRunTimeInMilliSeconds);
            Log.v(TAG, "Delta Sync: set lastSync in DeltaQuery to [" + lastRunTimeInMilliSeconds + "]");

        } catch (NoSuchFieldException e) {
            Log.v(TAG, "Delta Sync: field 'lastSync' not present in query. Skipping adjustment");
        } catch (IllegalAccessException e) {
            Log.v(TAG, "Delta Sync: Unable to override value in for 'lastSync'. Skipping adjustment");
        }
        return aq;
    }
}
