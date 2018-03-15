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

package com.apollographql.apollo.internal.subscription;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionResponse;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

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
    public void setStore(ApolloStore apolloStore) {

    }

    @Override
    public void setScalarTypeAdapters(ScalarTypeAdapters scalarTypeAdapters) {

    }
}
