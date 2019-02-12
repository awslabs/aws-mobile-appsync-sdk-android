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

package com.apollographql.apollo.internal;

import com.apollographql.apollo.GraphQLCall;
import com.amazonaws.mobileconnectors.appsync.AppSyncQueryWatcher;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;
import static com.apollographql.apollo.internal.CallState.TERMINATED;

final class RealAppSyncQueryWatcher<T> implements AppSyncQueryWatcher<T> {
  private RealAppSyncCall<T> activeCall;
  private ResponseFetcher refetchResponseFetcher = AppSyncResponseFetchers.CACHE_FIRST;
  private final ApolloStore mApolloStore;
  private Set<String> dependentKeys = Collections.emptySet();
  private final ApolloLogger logger;
  private final ApolloCallTracker tracker;
  private final ApolloStore.RecordChangeSubscriber recordChangeSubscriber = new ApolloStore.RecordChangeSubscriber() {
    @Override public void onCacheRecordsChanged(Set<String> changedRecordKeys) {
      if (!Utils.areDisjoint(dependentKeys, changedRecordKeys)) {
        refetch();
      }
    }
  };
  private final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
  private final AtomicReference<GraphQLCall.Callback<T>> originalCallback = new AtomicReference<>();

  RealAppSyncQueryWatcher(RealAppSyncCall<T> originalCall, ApolloStore apolloStore, ApolloLogger logger,
                          ApolloCallTracker tracker) {
    this.activeCall = originalCall;
    this.mApolloStore = apolloStore;
    this.logger = logger;
    this.tracker = tracker;
  }

  @Override public AppSyncQueryWatcher<T> enqueueAndWatch(@Nullable final GraphQLCall.Callback<T> callback) {
    try {
      activate(Optional.fromNullable(callback));
    } catch (ApolloCanceledException e) {
      if (callback != null) {
        callback.onCanceledError(e);
      } else {
        logger.e(e, "Operation: %s was canceled", operation().name().name());
      }
      return this;
    }
    activeCall.enqueue(callbackProxy());
    return this;
  }

  @Nonnull
  @Override public synchronized RealAppSyncQueryWatcher<T> refetchResponseFetcher(@Nonnull ResponseFetcher fetcher) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    checkNotNull(fetcher, "responseFetcher == null");
    this.refetchResponseFetcher = fetcher;
    return this;
  }

  @Override public synchronized void cancel() {
    switch (state.get()) {
      case ACTIVE:
        try {
          activeCall.cancel();
          mApolloStore.unsubscribe(recordChangeSubscriber);
        } finally {
          tracker.unregisterQueryWatcher(this);
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

  @Nonnull @Override public Operation operation() {
    return activeCall.operation();
  }

  @Override public synchronized void refetch() {
    switch (state.get()) {
      case ACTIVE:
        mApolloStore.unsubscribe(recordChangeSubscriber);
        activeCall.cancel();
        activeCall = activeCall.clone().responseFetcher(refetchResponseFetcher);
        activeCall.enqueue(callbackProxy());
        break;
      case IDLE:
        throw new IllegalStateException("Cannot refetch a watcher which has not first called enqueueAndWatch.");
      case CANCELED:
        throw new IllegalStateException("Cannot refetch a canceled watcher,");
      case TERMINATED:
        throw new IllegalStateException("Cannot refetch a watcher which has experienced an error.");
      default:
        throw new IllegalStateException("Unknown state");

    }

  }

  private GraphQLCall.Callback<T> callbackProxy() {
    return new GraphQLCall.Callback<T>() {
      @Override public void onResponse(@Nonnull Response<T> response) {
        Optional<GraphQLCall.Callback<T>> callback = responseCallback();
        if (!callback.isPresent()) {
          logger.d("onResponse for watched operation: %s. No callback present.", operation().name().name());
          return;
        }
        dependentKeys = response.dependentKeys();
        mApolloStore.subscribe(recordChangeSubscriber);
        callback.get().onResponse(response);
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        Optional<GraphQLCall.Callback<T>> callback = terminate();
        if (!callback.isPresent()) {
          logger.d(e, "onFailure for operation: %s. No callback present.", operation().name().name());
          return;
        }
        if (e instanceof ApolloHttpException) {
          callback.get().onHttpError((ApolloHttpException) e);
        } else if (e instanceof ApolloParseException) {
          callback.get().onParseError((ApolloParseException) e);
        } else if (e instanceof ApolloNetworkException) {
          callback.get().onNetworkError((ApolloNetworkException) e);
        } else {
          callback.get().onFailure(e);
        }
      }
    };
  }

  private synchronized void activate(Optional<GraphQLCall.Callback<T>> callback) throws ApolloCanceledException {
    switch (state.get()) {
      case IDLE:
        originalCallback.set(callback.orNull());
        tracker.registerQueryWatcher(this);
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

  private synchronized Optional<GraphQLCall.Callback<T>> responseCallback() {
    switch (state.get()) {
      case ACTIVE:
      case CANCELED:
        return Optional.fromNullable(originalCallback.get());
      case IDLE:
      case TERMINATED:
        throw new IllegalStateException(
            CallState.IllegalStateMessage.forCurrentState(state.get()).expected(ACTIVE, CANCELED));
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

  private synchronized Optional<GraphQLCall.Callback<T>> terminate() {
    switch (state.get()) {
      case ACTIVE:
        tracker.unregisterQueryWatcher(this);
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
