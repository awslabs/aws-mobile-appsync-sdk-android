/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.subscription;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionResponse;
import com.amazonaws.apollographql.apollo.api.Subscription;
import com.amazonaws.apollographql.apollo.cache.normalized.ApolloStore;
import com.amazonaws.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.amazonaws.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public final class NoOpSubscriptionManager implements SubscriptionManager {

    @Override
    public <T> void subscribe(@Nonnull Subscription<?, T, ?> subscription, @Nonnull List<String> subbedTopics, @Nonnull SubscriptionResponse response, ResponseNormalizer<Map<String, Object>> mapResponseNormalizer) {

    }

    @Override public void unsubscribe(@Nonnull Subscription<?, ?, ?> subscription) {
        throw new IllegalStateException("Subscription manager is not configured");
    }

    @Override
    public void addListener(Subscription subscription, AppSyncSubscriptionCall.Callback callback) {

    }

    @Override
    public void removeListener(Subscription subscription, AppSyncSubscriptionCall.Callback callback) {

    }

    @Override
    public void setStore(ApolloStore apolloStore) {

    }

    @Override
    public void setScalarTypeAdapters(ScalarTypeAdapters scalarTypeAdapters) {

    }
}
