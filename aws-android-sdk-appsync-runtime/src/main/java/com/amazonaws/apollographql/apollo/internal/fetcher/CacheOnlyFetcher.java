/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.fetcher;

import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptor;
import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.amazonaws.apollographql.apollo.api.Operation;
import com.amazonaws.apollographql.apollo.api.Response;
import com.amazonaws.apollographql.apollo.exception.ApolloException;
import com.amazonaws.apollographql.apollo.fetcher.ResponseFetcher;
import com.amazonaws.apollographql.apollo.internal.ApolloLogger;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * Signals the apollo client to <b>only</b> fetch the data from the normalized cache. If it's not present in the
 * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, an empty {@link
 * Response} is sent back with the {@link Operation} info
 * wrapped inside.
 */
public final class CacheOnlyFetcher implements ResponseFetcher {

  @Override public ApolloInterceptor provideInterceptor(ApolloLogger apolloLogger) {
    return new CacheOnlyInterceptor();
  }

  private static final class CacheOnlyInterceptor implements ApolloInterceptor {

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
        @Nonnull Executor dispatcher, @Nonnull final CallBack callBack) {
      InterceptorRequest cacheRequest = request.toBuilder().fetchFromCache(true).build();
      chain.proceedAsync(cacheRequest, dispatcher, new CallBack() {
        @Override public void onResponse(@Nonnull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          // Cache only returns null instead of throwing when the cache is empty
          callBack.onResponse(cacheMissResponse(request.operation));
          callBack.onCompleted();
        }

        @Override public void onCompleted() {
          callBack.onCompleted();
        }

        @Override public void onFetch(FetchSourceType sourceType) {
          callBack.onFetch(sourceType);
        }
      });
    }

    @Override public void dispose() {
      //no-op
    }

    private InterceptorResponse cacheMissResponse(Operation operation) {
      return new InterceptorResponse(null, Response.builder(operation).fromCache(true).build(), null);
    }
  }
}
