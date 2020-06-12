/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.fetcher;

import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptor;
import com.amazonaws.apollographql.apollo.fetcher.ResponseFetcher;
import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.amazonaws.apollographql.apollo.internal.ApolloLogger;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * Signals the apollo client to <b>only</b> fetch the GraphQL data from the network. If network request fails, an
 * exception is thrown.
 */
public final class NetworkOnlyFetcher implements ResponseFetcher {

  @Override public ApolloInterceptor provideInterceptor(ApolloLogger apolloLogger) {
    return new NetworkOnlyInterceptor();
  }

  private static final class NetworkOnlyInterceptor implements ApolloInterceptor {
    @Override
    public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
        @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
      InterceptorRequest networkRequest = request.toBuilder().fetchFromCache(false).build();
      chain.proceedAsync(networkRequest, dispatcher, callBack);
    }

    @Override public void dispose() {
    }
  }
}
