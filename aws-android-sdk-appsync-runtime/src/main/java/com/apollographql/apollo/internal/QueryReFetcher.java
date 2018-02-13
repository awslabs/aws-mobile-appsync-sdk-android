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

package com.apollographql.apollo.internal;

import com.apollographql.apollo.GraphQLCall;
import com.amazonaws.mobileconnectors.appsync.AppSyncQueryWatcher;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.exception.ApolloException;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import okhttp3.Call;
import okhttp3.HttpUrl;

final class QueryReFetcher {
  private final ApolloLogger logger;
  private final List<RealAppSyncCall> calls;
  private List<OperationName> queryWatchers;
  private ApolloCallTracker callTracker;
  private final AtomicBoolean executed = new AtomicBoolean();
  OnCompleteCallback onCompleteCallback;

  static Builder builder() {
    return new Builder();
  }

  QueryReFetcher(Builder builder) {
    logger = builder.logger;
    calls = new ArrayList<>(builder.queries.size());
    for (Query query : builder.queries) {
      calls.add(RealAppSyncCall.builder()
          .operation(query)
          .serverUrl(builder.serverUrl)
          .httpCallFactory(builder.httpCallFactory)
          .responseFieldMapperFactory(builder.responseFieldMapperFactory)
          .scalarTypeAdapters(builder.scalarTypeAdapters)
          .apolloStore(builder.mApolloStore)
          .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)
          .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
          .cacheHeaders(CacheHeaders.NONE)
          .logger(builder.logger)
          .applicationInterceptors(builder.applicationInterceptors)
          .tracker(builder.callTracker)
          .dispatcher(builder.dispatcher)
          .build());
    }
    queryWatchers = builder.queryWatchers;
    callTracker = builder.callTracker;
  }

  void refetch() {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }

    refetchQueryWatchers();
    refetchQueries();
  }

  void cancel() {
    for (RealAppSyncCall call : calls) {
      call.cancel();
    }
  }

  private void refetchQueryWatchers() {
    try {
      for (OperationName operationName : queryWatchers) {
        for (AppSyncQueryWatcher queryWatcher : callTracker.activeQueryWatchers(operationName)) {
          queryWatcher.refetch();
        }
      }
    } catch (Exception e) {
      logger.e(e, "Failed to re-fetch query watcher");
    }
  }

  private void refetchQueries() {
    final OnCompleteCallback completeCallback = onCompleteCallback;
    final AtomicInteger callsLeft = new AtomicInteger(calls.size());
    for (final RealAppSyncCall call : calls) {
      //noinspection unchecked
      call.enqueue(new GraphQLCall.Callback() {
        @Override public void onResponse(@Nonnull Response response) {
          if (callsLeft.decrementAndGet() == 0 && completeCallback != null) {
            completeCallback.onFetchComplete();
          }
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          if (logger != null) {
            logger.e(e, "Failed to fetch query: %s", call.operation);
          }

          if (callsLeft.decrementAndGet() == 0 && completeCallback != null) {
            completeCallback.onFetchComplete();
          }
        }
      });
    }
  }

  static final class Builder {
    List<Query> queries = Collections.emptyList();
    List<OperationName> queryWatchers = Collections.emptyList();
    HttpUrl serverUrl;
    Call.Factory httpCallFactory;
    ResponseFieldMapperFactory responseFieldMapperFactory;
    ScalarTypeAdapters scalarTypeAdapters;
    ApolloStore mApolloStore;
    Executor dispatcher;
    ApolloLogger logger;
    List<ApolloInterceptor> applicationInterceptors;
    ApolloCallTracker callTracker;

    Builder queries(List<Query> queries) {
      this.queries = queries != null ? queries : Collections.<Query>emptyList();
      return this;
    }

    public Builder queryWatchers(List<OperationName> queryWatchers) {
      this.queryWatchers = queryWatchers != null ? queryWatchers : Collections.<OperationName>emptyList();
      return this;
    }

    Builder serverUrl(HttpUrl serverUrl) {
      this.serverUrl = serverUrl;
      return this;
    }

    Builder httpCallFactory(Call.Factory httpCallFactory) {
      this.httpCallFactory = httpCallFactory;
      return this;
    }

    Builder responseFieldMapperFactory(ResponseFieldMapperFactory responseFieldMapperFactory) {
      this.responseFieldMapperFactory = responseFieldMapperFactory;
      return this;
    }

    Builder scalarTypeAdapters(ScalarTypeAdapters scalarTypeAdapters) {
      this.scalarTypeAdapters = scalarTypeAdapters;
      return this;
    }

    Builder apolloStore(ApolloStore apolloStore) {
      this.mApolloStore = apolloStore;
      return this;
    }

    Builder dispatcher(Executor dispatcher) {
      this.dispatcher = dispatcher;
      return this;
    }

    Builder logger(ApolloLogger logger) {
      this.logger = logger;
      return this;
    }

    Builder applicationInterceptors(List<ApolloInterceptor> applicationInterceptors) {
      this.applicationInterceptors = applicationInterceptors;
      return this;
    }

    Builder callTracker(ApolloCallTracker callTracker) {
      this.callTracker = callTracker;
      return this;
    }

    QueryReFetcher build() {
      return new QueryReFetcher(this);
    }

    private Builder() {
    }
  }

  interface OnCompleteCallback {
    void onFetchComplete();
  }
}
