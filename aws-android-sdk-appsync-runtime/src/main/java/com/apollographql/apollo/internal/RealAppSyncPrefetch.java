/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal;

import com.amazonaws.mobileconnectors.appsync.AppSyncPrefetch;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;

import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;
import static com.apollographql.apollo.internal.CallState.TERMINATED;

@SuppressWarnings("WeakerAccess") public final class RealAppSyncPrefetch implements AppSyncPrefetch {
  final Operation operation;
  final HttpUrl serverUrl;
  final Call.Factory httpCallFactory;
  final ScalarTypeAdapters scalarTypeAdapters;
  final Executor dispatcher;
  final ApolloLogger logger;
  final ApolloCallTracker tracker;
  final ApolloInterceptorChain interceptorChain;
  final boolean sendOperationIds;
  final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
  final AtomicReference<Callback> originalCallback = new AtomicReference<>();

  public RealAppSyncPrefetch(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory,
                             ScalarTypeAdapters scalarTypeAdapters, Executor dispatcher, ApolloLogger logger, ApolloCallTracker callTracker,
                             boolean sendOperationIds) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.httpCallFactory = httpCallFactory;
    this.scalarTypeAdapters = scalarTypeAdapters;
    this.dispatcher = dispatcher;
    this.logger = logger;
    this.tracker = callTracker;
    this.sendOperationIds = sendOperationIds;
    interceptorChain = new RealApolloInterceptorChain(Collections.<ApolloInterceptor>singletonList(
        new ApolloServerInterceptor(serverUrl, httpCallFactory, HttpCachePolicy.NETWORK_ONLY, true,
            scalarTypeAdapters, logger, sendOperationIds)
    ));
  }

  @Override public void enqueue(@Nullable final Callback responseCallback) {
    try {
      activate(Optional.fromNullable(responseCallback));
    } catch (ApolloCanceledException e) {
      if (responseCallback != null) {
        responseCallback.onFailure(e);
      } else {
        logger.e(e, "Operation: %s was canceled", operation().name().name());
      }
      return;
    }

    ApolloInterceptor.InterceptorRequest request = ApolloInterceptor.InterceptorRequest.builder(operation).build();
    interceptorChain.proceedAsync(request, dispatcher, interceptorCallbackProxy());
  }

  @Nonnull @Override public Operation operation() {
    return operation;
  }

  private ApolloInterceptor.CallBack interceptorCallbackProxy() {
    return new ApolloInterceptor.CallBack() {
      @Override public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        Response httpResponse = response.httpResponse.get();
        try {
          Optional<Callback> callback = terminate();
          if (!callback.isPresent()) {
            logger.d("onResponse for prefetch operation: %s. No callback present.", operation().name().name());
            return;
          }
          if (httpResponse.isSuccessful()) {
            callback.get().onSuccess();
          } else {
            callback.get().onHttpError(new ApolloHttpException(httpResponse));
          }
        } finally {
          httpResponse.close();
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        Optional<Callback> callback = terminate();
        if (!callback.isPresent()) {
          logger.e(e, "onFailure for prefetch operation: %s. No callback present.", operation().name().name());
          return;
        }
        if (e instanceof ApolloHttpException) {
          callback.get().onHttpError((ApolloHttpException) e);
        } else if (e instanceof ApolloNetworkException) {
          callback.get().onNetworkError((ApolloNetworkException) e);
        } else {
          callback.get().onFailure(e);
        }

      }

      @Override public void onCompleted() {
        // Prefetch is only called with NETWORK_ONLY, so callback api does not need onComplete as it is the same as
        // onResponse.
      }

      @Override public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {
      }
    };
  }

  @Override public AppSyncPrefetch clone() {
    return new RealAppSyncPrefetch(operation, serverUrl, httpCallFactory, scalarTypeAdapters, dispatcher,
        logger, tracker, sendOperationIds);
  }

  @Override public synchronized void cancel() {
    switch (state.get()) {
      case ACTIVE:
        try {
          interceptorChain.dispose();
        } finally {
          tracker.unregisterPrefetchCall(this);
          originalCallback.set(null);
          state.set(CANCELED);
        }
        break;
      case IDLE:
        state.set(CANCELED);
        break;
      case CANCELED:
      case TERMINATED:
        // These are not illegal states, but cancelling does nothing
        break;
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

  @Override public boolean isCanceled() {
    return state.get() == CANCELED;
  }

  private synchronized void activate(Optional<Callback> callback) throws ApolloCanceledException {
    switch (state.get()) {
      case IDLE:
        originalCallback.set(callback.orNull());
        tracker.registerPrefetchCall(this);
        break;
      case CANCELED:
        throw new ApolloCanceledException("Call is cancelled.");
      case TERMINATED:
      case ACTIVE:
        throw new IllegalStateException("Already Executed");
      default:
        throw new IllegalStateException("Unknown state");
    }
    state.set(ACTIVE);
  }

  private synchronized Optional<Callback> terminate() {
    switch (state.get()) {
      case ACTIVE:
        tracker.unregisterPrefetchCall(this);
        state.set(TERMINATED);
        return Optional.fromNullable(originalCallback.getAndSet(null));
      case CANCELED:
        return Optional.fromNullable(originalCallback.getAndSet(null));
      case IDLE:
      case TERMINATED:
        throw new IllegalStateException(
            CallState.IllegalStateMessage.forCurrentState(state.get()).expected(ACTIVE, CANCELED));
      default:
        throw new IllegalStateException("Unknown state");
    }
  }
}
