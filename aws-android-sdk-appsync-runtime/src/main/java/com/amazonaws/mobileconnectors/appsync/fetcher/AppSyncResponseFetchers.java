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

package com.amazonaws.mobileconnectors.appsync.fetcher;

import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.fetcher.CacheFirstFetcher;
import com.apollographql.apollo.internal.fetcher.CacheOnlyFetcher;
import com.apollographql.apollo.internal.fetcher.CacheAndNetworkFetcher;
import com.apollographql.apollo.internal.fetcher.NetworkFirstFetcher;
import com.apollographql.apollo.internal.fetcher.NetworkOnlyFetcher;

public final class AppSyncResponseFetchers {

  /**
   * Signals the appsync client to <b>only</b> fetch the data from the normalized cache. If it's not present in
   * the normalized cache or if an exception occurs while trying to fetch it from the normalized cache, an empty {@link
   * com.apollographql.apollo.api.Response} is sent back with the {@link com.apollographql.apollo.api.Operation} info
   * wrapped inside.
   */
  public static final ResponseFetcher CACHE_ONLY = new CacheOnlyFetcher();

  /**
   * Signals the appsync client to <b>only</b> fetch the GraphQL data from the network. If network request fails, an
   * exception is thrown.
   */
  public static final ResponseFetcher NETWORK_ONLY = new NetworkOnlyFetcher();

  /**
   * Signals the appsync client to first fetch the data from the normalized cache. If it's not present in the
   * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is
   * instead fetched from the network.
   */
  public static final ResponseFetcher CACHE_FIRST = new CacheFirstFetcher();

  /**
   * Signals the appsync client to first fetch the data from the network. If network request fails, then the
   * data is fetched from the normalized cache. If the data is not present in the normalized cache, then the
   * exception which led to the network request failure is rethrown.
   */
  public static final ResponseFetcher NETWORK_FIRST = new NetworkFirstFetcher();

  /**
   * Signal the appsync client to fetch the data from both the network and the cache. If cached data is not
   * present, only network data will be returned. If cached data is available, but network experiences an error,
   * cached data is returned. If cache data is not available, and network data is not available, the error
   * of the network request will be propagated. If both network and cache are available, both will be returned.
   * Cache data is guaranteed to be returned first.
   */
  public static final ResponseFetcher CACHE_AND_NETWORK = new CacheAndNetworkFetcher();
}
