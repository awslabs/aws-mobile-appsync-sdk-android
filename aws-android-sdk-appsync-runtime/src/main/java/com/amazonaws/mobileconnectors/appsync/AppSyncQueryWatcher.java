/**
 * Copyright 2018-2019 Amazon.com,
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

import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AppSyncQueryWatcher<T> extends Cancelable {

  AppSyncQueryWatcher<T> enqueueAndWatch(@Nullable final GraphQLCall.Callback<T> callback);

  /**
   * @param fetcher The {@link ResponseFetcher} to use when the call is refetched due to a field changing in the
   *                     cache.
   */
  @Nonnull
  AppSyncQueryWatcher<T> refetchResponseFetcher(@Nonnull ResponseFetcher fetcher);

  /**
   * Returns GraphQL watched operation.
   *
   * @return {@link Operation}
   */
  @Nonnull Operation operation();

  /**
   * Re-fetches watched GraphQL query.
   */
  void refetch();

  /**
   * Cancels this {@link AppSyncQueryWatcher}. The {@link GraphQLCall.Callback}
   * will be disposed, and will receive no more events. Any active operations will attempt to abort and
   * release resources, if possible.
   */
  @Override void cancel();

}
