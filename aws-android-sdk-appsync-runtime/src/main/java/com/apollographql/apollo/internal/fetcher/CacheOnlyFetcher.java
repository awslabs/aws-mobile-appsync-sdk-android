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

package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.ApolloLogger;

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