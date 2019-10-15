/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;

public class RealAppSyncSubscriptionCall<T> implements AppSyncSubscriptionCall<T> {

    public static Semaphore subscriptionSemaphore = new Semaphore(1, true);
    private static int MAX_WAIT_TIME = 30;

    private final ApolloLogger logger;
    private final Subscription<?, T, ?> subscription;
    private final SubscriptionManager subscriptionManager;
    private final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
    private final ApolloClient apolloClient;
    private final RealAppSyncCall<T> subscriptionMetadataRequest;
    private Callback<T> userCallback;
    private static final String TAG = RealAppSyncSubscriptionCall.class.getSimpleName();


    public RealAppSyncSubscriptionCall(
            Subscription<?, T, ?> subscription,
            SubscriptionManager subscriptionManager,
            ApolloClient apolloClient,
            ApolloLogger logger,
            RealAppSyncCall<T> solicitingCall) {
        this.subscription = subscription;
        this.subscriptionManager = subscriptionManager;
        this.apolloClient = apolloClient;
        this.subscriptionMetadataRequest = solicitingCall;
        this.logger = logger;
    }

    @Override
    public synchronized void execute(@Nonnull final Callback<T> callback) {
        if ( callback == null ) {
            logger.w("Subscription Infrastructure: Callback passed into subscription [" + subscription +"] was null. Will not subscribe.");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                userCallback = callback;
                subscriptionManager.addListener(subscription, callback);

                switch (state.get()) {
                    case IDLE: {
                        state.set(ACTIVE);
                        break;
                    }

                    case CANCELED:
                        callback.onFailure(new ApolloCanceledException("Call is cancelled."));
                        break;

                    case ACTIVE:
                        callback.onFailure(new ApolloException("Already Executed"));
                        break;

                    default:
                        callback.onFailure(new ApolloException("Unknown state"));
                }

                try {
                    if (subscriptionSemaphore.tryAcquire(MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                        logger.d("Subscription Infrastructure: Acquired subscription Semaphore. Continuing");
                    } else {
                        logger.d("Subscription Infrastructure: Did not acquire subscription Semaphore after waiting for [" + MAX_WAIT_TIME + "] seconds. Will continue");

                    }
                } catch (InterruptedException e) {
                    logger.e(e, "Subscription Infrastructure:Got exception while waiting to acquire subscription Semaphore. Will continue without waiting");
                }


                logger.d("Subscription Infrastructure: Making request to server to get Subscription Meta Data");
                subscriptionMetadataRequest.enqueue(new GraphQLCall.Callback<T>() {
                    @Override
                    public void onResponse(@Nonnull Response<T> response) {
                        subscriptionSemaphore.release();
                        // Do nothing. Internal code has been kicked off.
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        subscriptionSemaphore.release();
                        reportFailureToSubscriptionManager();
                        callback.onFailure(e);
                    }
                });
            }
        }).start();
    }

    void  reportFailureToSubscriptionManager () {
        logger.d("Trying to report failure to Subscription Manager");
        try {
            //Use Reflection to call reportConnectionError on RealSubscriptionManager
            Method method = subscriptionManager.getClass().getDeclaredMethod("reportConnectionError");
            method.invoke(subscriptionManager);
        }
        catch (NoSuchMethodException noe ){
            logger.d("Exception [" + noe + "] trying to call reportConnectionError in subscriptionManager");
        }
        catch (InvocationTargetException ite) {
            logger.d("Exception [" + ite + "] trying to call reportConnectionError in subscriptionManager");
        }
        catch (IllegalAccessException iae) {
            logger.d("Exception [" + iae + "] trying to call reportConnectionError in subscriptionManager");
        }
    }


    @Override
    public void cancel() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Cancel subscription only if in Active state.
                synchronized (this) {
                    switch (state.get()) {
                        case IDLE: {
                            state.set(CANCELED);
                            break;
                        }

                        case ACTIVE: {
                            try {
                                subscriptionManager.unsubscribe(subscription);
                                subscriptionManager.removeListener(subscription, userCallback);
                                if (userCallback != null ) {
                                    userCallback.onCompleted();
                                    userCallback = null;
                                }
                            } finally {
                                state.set(CANCELED);
                            }
                            break;
                        }

                        case CANCELED:
                            // These are not illegal states, but cancelling does nothing
                            break;

                        default:
                            throw new IllegalStateException("Unknown state");
                    }
                }
            }
        }).start();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AppSyncSubscriptionCall<T> clone() {
        return new RealAppSyncSubscriptionCall<>(subscription, subscriptionManager, apolloClient, logger, subscriptionMetadataRequest.clone());
    }

    @Override public boolean isCanceled() {
        return state.get() == CANCELED;
    }
}
