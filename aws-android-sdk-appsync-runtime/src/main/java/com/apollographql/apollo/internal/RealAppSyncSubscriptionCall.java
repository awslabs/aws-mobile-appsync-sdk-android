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

package com.apollographql.apollo.internal;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;

public class RealAppSyncSubscriptionCall<T> implements AppSyncSubscriptionCall<T> {

    public static Semaphore subscriptionSemaphore = new Semaphore(1);
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
    public void execute(@Nonnull final Callback<T> callback) {
        if ( callback == null ) {
            logger.w("Subscription Infrastructure: Callback passed into subscription [" + subscription +"] was null. Will not subscribe.");
            return;
        }
        userCallback = callback;
        subscriptionManager.addListener(subscription, callback);

        //Ensure that the call is only made once.
        synchronized (this) {
            switch (state.get()) {
                case IDLE: {
                    state.set(ACTIVE);
                    break;
                }

                case CANCELED:
                    throw new RuntimeException("Cancelled", new ApolloCanceledException("Call is cancelled."));

                case ACTIVE:
                    throw new IllegalStateException("Already Executed");

                default:
                    throw new IllegalStateException("Unknown state");
            }
        }

        try {
            if (subscriptionSemaphore.tryAcquire(MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                logger.d("Subscription Infrastructure: Acquired subscription Semaphore. Continuing");
                System.out.println("Subscription Infrastructure: Acquired subscription Semaphore. Continuing");
            } else {
                logger.d("Subscription Infrastructure: Did not acquire subscription Semaphore after waiting for [" + MAX_WAIT_TIME + "] seconds. Will continue");
                System.out.println("Subscription Infrastructure: Did not acquire subscription Semaphore after waiting for [" + MAX_WAIT_TIME + "] seconds. Will continue");

            }
        } catch (InterruptedException e) {
            logger.e(e, "Subscription Infrastructure:Got exception while waiting to acquire subscription Semaphore. Will continue without waiting");
            System.out.println("Subscription Infrastructure:Got exception while waiting to acquire subscription Semaphore. Will continue without waiting");
        }


        logger.d("Subscription Infrastructure: Making request to server to get Subscription Meta Data");
        this.subscriptionMetadataRequest.enqueue(new GraphQLCall.Callback<T>() {
            @Override
            public void onResponse(@Nonnull Response<T> response) {
                System.out.println("Subscription Infrastructure: On Response called for Subscription Meta Data request");
                subscriptionSemaphore.release();
                // Do nothing. Internal code has been kicked off.
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                subscriptionSemaphore.release();
                System.out.println("Subscription Infrastructure: On Failure called for Subscription Meta Data request");
                callback.onFailure(e);
            }
        });
    }

    @Override
    public void cancel() {
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
                        userCallback.onCompleted();
                        userCallback = null;
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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AppSyncSubscriptionCall<T> clone() {
        return new RealAppSyncSubscriptionCall<>(subscription, subscriptionManager, apolloClient, logger, subscriptionMetadataRequest.clone());
    }

    @Override public boolean isCanceled() {
        return state.get() == CANCELED;
    }
}
