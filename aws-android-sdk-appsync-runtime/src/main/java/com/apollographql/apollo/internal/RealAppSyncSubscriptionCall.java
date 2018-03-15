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

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;

public class RealAppSyncSubscriptionCall<T> implements AppSyncSubscriptionCall<T> {
    private final Subscription<?, T, ?> subscription;
    private final SubscriptionManager subscriptionManager;
    private final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
    private final ApolloClient apolloClient;
    private final RealAppSyncCall<T> solicitingCall;

    public RealAppSyncSubscriptionCall(
            Subscription<?, T, ?> subscription,
            SubscriptionManager subscriptionManager,
            ApolloClient apolloClient,
            RealAppSyncCall<T> solicitingCall) {
        this.subscription = subscription;
        this.subscriptionManager = subscriptionManager;
        this.apolloClient = apolloClient;
        this.solicitingCall = solicitingCall;
    }

    @Override
    public void execute(@Nonnull final Callback<T> callback) {
        checkNotNull(callback, "callback == null");
        subscriptionManager.addListener(subscription, callback);
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
        this.solicitingCall.enqueue(new GraphQLCall.Callback<T>() {
            @Override
            public void onResponse(@Nonnull Response<T> response) {
                // Do nothing. Internal code has been kicked off.
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                callback.onFailure(e);
            }
        });
    }

    @Override
    public void cancel() {
        synchronized (this) {
            switch (state.get()) {
                case IDLE: {
                    state.set(CANCELED);
                    break;
                }

                case ACTIVE: {
                    try {
                        subscriptionManager.unsubscribe(subscription);
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
        return new RealAppSyncSubscriptionCall<>(subscription, subscriptionManager, apolloClient, solicitingCall.clone());
    }

    @Override public boolean isCanceled() {
        return state.get() == CANCELED;
    }
}
