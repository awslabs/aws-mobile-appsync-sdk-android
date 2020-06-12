/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.fetcher;

import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptor;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.apollographql.apollo.internal.ApolloLogger;

/**
 * A ResponseFetcher is an {@link ApolloInterceptor} inserted at the beginning of a request chain.
 * It can control how a request is fetched by configuring {@link com.apollographql.apollo.interceptor.FetchOptions}.
 *
 * See {@link AppSyncResponseFetchers} for a basic set of fetchers.
 */
public interface ResponseFetcher {

  /**
   * @param logger A {@link ApolloLogger} to log relevant fetch information.
   * @return The {@link ApolloInterceptor} that executes the fetch logic.
   */
  ApolloInterceptor provideInterceptor(ApolloLogger logger);

}
