/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.response.OperationResponseParser;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * ApolloParseInterceptor is a concrete {@link ApolloInterceptor} responsible for inflating the http responses into
 * models. To get the http responses, it hands over the control to the next interceptor in the chain and proceeds to
 * then parse the returned response.
 */
public final class ApolloParseInterceptor implements ApolloInterceptor {
  private final HttpCache httpCache;
  private final ResponseNormalizer<Map<String, Object>> normalizer;
  private final ResponseFieldMapper responseFieldMapper;
  private final ScalarTypeAdapters scalarTypeAdapters;
  private final ApolloLogger logger;
  private volatile boolean disposed;

  public ApolloParseInterceptor(HttpCache httpCache, ResponseNormalizer<Map<String, Object>> normalizer,
                                ResponseFieldMapper responseFieldMapper, ScalarTypeAdapters scalarTypeAdapters, ApolloLogger logger) {
    this.httpCache = httpCache;
    this.normalizer = normalizer;
    this.responseFieldMapper = responseFieldMapper;
    this.scalarTypeAdapters = scalarTypeAdapters;
    this.logger = logger;
  }

  @Override
  public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
      @Nonnull Executor dispatcher, @Nonnull final CallBack callBack) {
    if (disposed) return;
//    if (request.operation instanceof Subscription) chain.proceedAsync(request, dispatcher, callBack);
    chain.proceedAsync(request, dispatcher, new CallBack() {
      @Override public void onResponse(@Nonnull InterceptorResponse response) {
        try {
          if (disposed) return;
          if (response.parsedResponse.isPresent()) {
            callBack.onResponse(response);
          } else {
            InterceptorResponse result = parse(request.operation, response.httpResponse.get());
            callBack.onResponse(result);
          }
          callBack.onCompleted();
        } catch (ApolloException e) {
          onFailure(e);
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        if (disposed) return;
        callBack.onFailure(e);
      }

      @Override public void onCompleted() {
        // call onCompleted in onResponse in case of error
      }

      @Override public void onFetch(FetchSourceType sourceType) {
        callBack.onFetch(sourceType);
      }
    });
  }

  @Override public void dispose() {
    disposed = true;
  }

  @SuppressWarnings("unchecked") private InterceptorResponse parse(Operation operation, okhttp3.Response httpResponse)
      throws ApolloHttpException, ApolloParseException {
    String cacheKey = httpResponse.request().header(HttpCache.CACHE_KEY_HEADER);

    String cloneString = null;
    ResponseBody responseBody = httpResponse.body();
    BufferedSource source = responseBody.source();
    try {
      source.request(Long.MAX_VALUE); // request the entire body.
      Buffer buffer = source.buffer();
      cloneString = buffer.clone().readString(Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (httpResponse.isSuccessful()) {
      try {
        OperationResponseParser parser = new OperationResponseParser(operation, responseFieldMapper, scalarTypeAdapters,
            normalizer);
        Response parsedResponse = parser.parse(httpResponse.body().source())
            .toBuilder()
            .fromCache(httpResponse.cacheResponse() != null)
            .build();
        if (parsedResponse.hasErrors() && httpCache != null) {
          httpCache.removeQuietly(cacheKey);
        }
        return new InterceptorResponse(httpResponse, parsedResponse, normalizer.records(), cloneString);
      } catch (Exception rethrown) {
        logger.e(rethrown, "Failed to parse network response for operation: %s", operation);
        closeQuietly(httpResponse);
        if (httpCache != null) {
          httpCache.removeQuietly(cacheKey);
        }
        throw new ApolloParseException("Failed to parse http response", rethrown);
      }
    } else {
      logger.e("Failed to parse network response: %s", httpResponse);
      throw new ApolloHttpException(httpResponse);
    }
  }

  private static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception ignored) {
      }
    }
  }
}
