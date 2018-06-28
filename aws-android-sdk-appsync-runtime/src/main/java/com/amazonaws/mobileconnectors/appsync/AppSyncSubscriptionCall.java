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
}