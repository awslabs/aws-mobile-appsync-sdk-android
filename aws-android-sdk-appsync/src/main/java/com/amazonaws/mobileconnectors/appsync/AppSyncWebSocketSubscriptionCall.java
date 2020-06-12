/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.annotation.NonNull;

import com.amazonaws.apollographql.apollo.api.Operation;
import com.amazonaws.apollographql.apollo.api.Subscription;

/**
 * Represents a client call to subscribe. It can be used to execute and cancel a GraphQL subscription.
 * The subscription messages are delivered over a WebSocket connection in accordance with ApolloGraphQL protocol.
 *
 * @see <a href="https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md">ApolloGraphQL protocol</a>
 * @param <D>  Response data class for the GraphQL Operation
 * @param <T>  Callback data type. This is usually same as {@link D}, the response data type
 * @param <V>  Variables associated with the GraphQL Operations
 */
final class AppSyncWebSocketSubscriptionCall<D extends Operation.Data, T, V extends Operation.Variables> implements AppSyncSubscriptionCall<T> {

    private final Subscription<D, T, V> subscription;
    private final WebSocketConnectionManager websocketConnectionManager;
    private String subscriptionId;
    private Callback<T> callback;
    private boolean isCanceled;

    AppSyncWebSocketSubscriptionCall(
            Subscription<D, T, V> subscription,
            WebSocketConnectionManager websocketConnectionManager) {
        this.subscription = subscription;
        this.websocketConnectionManager = websocketConnectionManager;
        this.callback = null;
        this.isCanceled = false;
        this.callback = null;
    }

    @Override
    public synchronized void execute(@NonNull Callback<T> callback) {
        if (this.callback != null) {
            throw new IllegalStateException("Subscription call has already been executed.");
        }
        this.callback = callback;
        this.subscriptionId = websocketConnectionManager.requestSubscription(subscription, callback);
    }

    @Override
    public AppSyncSubscriptionCall<T> clone() {
        return new AppSyncWebSocketSubscriptionCall<>(subscription, websocketConnectionManager);
    }

    @Override
    public synchronized void cancel() {
        this.isCanceled = true;
        websocketConnectionManager.releaseSubscription(subscriptionId);
        this.callback.onCompleted();
    }

    @Override
    public synchronized boolean isCanceled() {
        return isCanceled;
    }
}
