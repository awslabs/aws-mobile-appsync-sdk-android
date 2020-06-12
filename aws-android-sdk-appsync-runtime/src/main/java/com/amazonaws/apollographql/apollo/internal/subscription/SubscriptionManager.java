/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.subscription;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionResponse;
import com.amazonaws.apollographql.apollo.api.Response;
import com.amazonaws.apollographql.apollo.api.Subscription;
import com.amazonaws.apollographql.apollo.cache.normalized.ApolloStore;
import com.amazonaws.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.amazonaws.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public interface SubscriptionManager {

    /**
     * Make a connection and subscribe to all topics.
     * @param <T> The type of the repsonse data
     * @param subscription The operation that has parsing logic, operation id, etc.
     * @param subbedTopics The topics relevant to the subscription.
     * @param response list of mqtt connections and topics
     * @param mapResponseNormalizer
     */
    <T> void subscribe(
            @Nonnull Subscription<?, T, ?> subscription,
            @Nonnull final List<String> subbedTopics,
            @Nonnull SubscriptionResponse response,
            ResponseNormalizer<Map<String, Object>> mapResponseNormalizer);

    void unsubscribe(@Nonnull Subscription<?, ?, ?> subscription);

    /**
     * Sets the listener based on the subscription's operation (unique) id
     * @param subscription The subscription for which callbacks will be triggered.
     * @param callback The callback for messages received on the subscription connection.
     */
    void addListener(Subscription subscription, AppSyncSubscriptionCall.Callback callback);

    /**
     * Removes the listener based on the subscription's operation (unique) id
     * @param subscription The subscription for which callbacks will be triggered.
     * @param callback The callback for messages received on the subscription connection.
     */
    void removeListener(Subscription subscription, AppSyncSubscriptionCall.Callback callback);

    /**
     * Sets the store used for cache updates based on subscription responses.
     * @param apolloStore
     */
    void setStore(ApolloStore apolloStore);

    void setScalarTypeAdapters(ScalarTypeAdapters scalarTypeAdapters);

    interface Callback<T> {
        void onResponse(@Nonnull Response<T> response);

        void onError(@Nonnull Exception error);

        void onNetworkError(@Nonnull Throwable t);

        void onCompleted();
    }
}
