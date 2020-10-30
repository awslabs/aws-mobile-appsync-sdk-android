/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;

public interface AppSyncSubscriptionCall<T> extends Cancelable {
    void execute(@Nonnull Callback<T> callback);

    AppSyncSubscriptionCall<T> clone();

    interface Factory {
        <D extends Subscription.Data, T, V extends Subscription.Variables> AppSyncSubscriptionCall<T> subscribe(
                @Nonnull Subscription<D, T, V> subscription);
    }

    interface Callback<T> {

        /**
        This method is called every time a message is received.
         */
        void onResponse(@Nonnull Response<T> response);

        /**
        This method is called if there is an error creating subscription or parsing server response.
         */
        void onFailure(@Nonnull ApolloException e);

        /**
         This method is called when a subscription is terminated.
         */
        void onCompleted();
    }

    interface StartedCallback<T> extends Callback<T> {

        /**
         This method is called when a subscription first connects successfully.
         */
        void onStarted();
    }
}