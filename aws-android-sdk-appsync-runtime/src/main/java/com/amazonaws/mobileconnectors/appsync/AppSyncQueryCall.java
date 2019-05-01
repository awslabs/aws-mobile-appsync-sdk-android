/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.cache.GraphQLCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import javax.annotation.Nonnull;

/**
 * A call prepared to execute GraphQL query operation.
 */
public interface AppSyncQueryCall<T> extends GraphQLCall<T> {
  /**
   * Returns a watcher to watch the changes to the normalized cache records this query depends on or when mutation call
   * triggers to re-fetch this query after it completes via {@link AppSyncMutationCall#refetchQueries(OperationName...)}
   *
   * @return {@link AppSyncQueryWatcher}
   */
  @Nonnull
  AppSyncQueryWatcher<T> watcher();

  /**
   * Sets the http cache policy for response/request cache.
   *
   * @param httpCachePolicy {@link HttpCachePolicy.Policy} to set
   * @return {@link AppSyncQueryCall} with the provided {@link HttpCachePolicy.Policy}
   */
  @Nonnull
  AppSyncQueryCall<T> httpCachePolicy(@Nonnull HttpCachePolicy.Policy httpCachePolicy);

  /**
   * Sets the {@link CacheHeaders} to use for this call. {@link FetchOptions} will
   * be configured with this headers, and will be accessible from the {@link ResponseFetcher} used for this call.
   *
   * @param cacheHeaders the {@link CacheHeaders} that will be passed with records generated from this request to {@link
   *                     NormalizedCache}. Standardized cache headers are
   *                     defined in {@link GraphQLCacheHeaders}.
   * @return The GraphQLCall object with the provided {@link CacheHeaders}.
   */
  @Nonnull @Override
  AppSyncQueryCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders);

  /**
   * Sets the {@link ResponseFetcher} strategy for an GraphQLCall object.
   *
   * @param fetcher the {@link ResponseFetcher} to use.
   * @return The GraphQLCall object with the provided CacheControl strategy
   */
  @Nonnull
  AppSyncQueryCall<T> responseFetcher(@Nonnull ResponseFetcher fetcher);

  @Nonnull @Override
  AppSyncQueryCall<T> clone();

  /**
   * Factory for creating {@link AppSyncQueryCall} calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new {@link AppSyncQueryCall} call.
     *
     * @param query the operation which needs to be performed
     * @return prepared {@link AppSyncQueryCall} call to be executed at some point in the future
     */
    <D extends Query.Data, T, V extends Query.Variables> AppSyncQueryCall<T> query(@Nonnull Query<D, T, V> query);
  }
}
