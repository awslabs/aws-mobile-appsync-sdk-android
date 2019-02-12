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

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.GraphQLCall;
import com.amazonaws.mobileconnectors.appsync.AppSyncMutationCall;
import com.amazonaws.mobileconnectors.appsync.AppSyncQueryCall;
import com.amazonaws.mobileconnectors.appsync.AppSyncPrefetch;
import com.amazonaws.mobileconnectors.appsync.AppSyncQueryWatcher;
import com.apollographql.apollo.IdleResourceCallback;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Subscription;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloCallTracker is responsible for keeping track of running {@link AppSyncPrefetch} & {@link AppSyncQueryCall}
 * & {@link AppSyncMutationCall} & {@link AppSyncQueryWatcher} calls.
 */
@SuppressWarnings("WeakerAccess") public final class ApolloCallTracker {
  private final Map<OperationName, Set<AppSyncPrefetch>> activePrefetchCalls = new HashMap<>();
  private final Map<OperationName, Set<AppSyncQueryCall>> activeQueryCalls = new HashMap<>();
  private final Map<OperationName, Set<AppSyncMutationCall>> activeMutationCalls = new HashMap<>();
  private final Map<OperationName, Set<AppSyncQueryWatcher>> activeQueryWatchers = new HashMap<>();
  private final AtomicInteger activeCallCount = new AtomicInteger();

  private IdleResourceCallback idleResourceCallback;

  public ApolloCallTracker() {
  }

  /**
   * <p>Adds provided {@link GraphQLCall} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  void registerCall(@Nonnull GraphQLCall call) {
    checkNotNull(call, "call == null");
    Operation operation = call.operation();
    if (operation instanceof Query) {
      registerQueryCall((AppSyncQueryCall) call);
    } else if (operation instanceof Mutation) {
      registerMutationCall((AppSyncMutationCall) call);
    } else if (operation instanceof Subscription) {
    } else {
      throw new IllegalArgumentException("Unknown call type");
    }
  }

  /**
   * <p>Removes provided {@link GraphQLCall} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterCall(@Nonnull GraphQLCall call) {
    checkNotNull(call, "call == null");
    Operation operation = call.operation();
    if (operation instanceof Query) {
      unregisterQueryCall((AppSyncQueryCall) call);
    } else if (operation instanceof Mutation) {
      unregisterMutationCall((AppSyncMutationCall) call);
    } else if (operation instanceof Subscription) {
    } else {
      throw new IllegalArgumentException("Unknown call type");
    }
  }

  /**
   * <p>Adds provided {@link AppSyncPrefetch} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before a prefetch call is executed.</p>
   */
  void registerPrefetchCall(@Nonnull AppSyncPrefetch appSyncPrefetch) {
    checkNotNull(appSyncPrefetch, "appSyncPrefetch == null");
    OperationName operationName = appSyncPrefetch.operation().name();
    registerCall(activePrefetchCalls, operationName, appSyncPrefetch);
  }

  /**
   * <p>Removes provided {@link AppSyncPrefetch} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after a prefetch call is completed (whether successful or
   * failed).</p>
   */
  void unregisterPrefetchCall(@Nonnull AppSyncPrefetch appSyncPrefetch) {
    checkNotNull(appSyncPrefetch, "appSyncPrefetch == null");
    OperationName operationName = appSyncPrefetch.operation().name();
    unregisterCall(activePrefetchCalls, operationName, appSyncPrefetch);
  }

  /**
   * Returns currently active {@link AppSyncPrefetch} calls by operation name.
   *
   * @param operationName prefetch operation name
   * @return set of active prefetch calls
   */
  @Nonnull Set<AppSyncPrefetch> activePrefetchCalls(@Nonnull OperationName operationName) {
    return activeCalls(activePrefetchCalls, operationName);
  }

  /**
   * <p>Adds provided {@link AppSyncQueryCall} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  void registerQueryCall(@Nonnull AppSyncQueryCall appSyncQueryCall) {
    checkNotNull(appSyncQueryCall, "appSyncQueryCall == null");
    OperationName operationName = appSyncQueryCall.operation().name();
    registerCall(activeQueryCalls, operationName, appSyncQueryCall);
  }

  /**
   * <p>Removes provided {@link AppSyncQueryCall} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterQueryCall(@Nonnull AppSyncQueryCall appSyncQueryCall) {
    checkNotNull(appSyncQueryCall, "appSyncQueryCall == null");
    OperationName operationName = appSyncQueryCall.operation().name();
    unregisterCall(activeQueryCalls, operationName, appSyncQueryCall);
  }

  /**
   * Returns currently active {@link AppSyncQueryCall} calls by operation name.
   *
   * @param operationName query operation name
   * @return set of active query calls
   */
  @Nonnull Set<AppSyncQueryCall> activeQueryCalls(@Nonnull OperationName operationName) {
    return activeCalls(activeQueryCalls, operationName);
  }

  /**
   * <p>Adds provided {@link AppSyncMutationCall} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  void registerMutationCall(@Nonnull AppSyncMutationCall appSyncMutationCall) {
    checkNotNull(appSyncMutationCall, "appSyncMutationCall == null");
    OperationName operationName = appSyncMutationCall.operation().name();
    registerCall(activeMutationCalls, operationName, appSyncMutationCall);
  }

  /**
   * <p>Removes provided {@link AppSyncMutationCall} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterMutationCall(@Nonnull AppSyncMutationCall appSyncMutationCall) {
    checkNotNull(appSyncMutationCall, "appSyncMutationCall == null");
    OperationName operationName = appSyncMutationCall.operation().name();
    unregisterCall(activeMutationCalls, operationName, appSyncMutationCall);
  }

  /**
   * Returns currently active {@link AppSyncMutationCall} calls by operation name.
   *
   * @param operationName query operation name
   * @return set of active mutation calls
   */
  @Nonnull Set<AppSyncMutationCall> activeMutationCalls(@Nonnull OperationName operationName) {
    return activeCalls(activeMutationCalls, operationName);
  }

  /**
   * <p>Adds provided {@link AppSyncQueryWatcher} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before
   * {@link AppSyncQueryWatcher#enqueueAndWatch(GraphQLCall.Callback)}.</p>
   */
  void registerQueryWatcher(@Nonnull AppSyncQueryWatcher queryWatcher) {
    checkNotNull(queryWatcher, "queryWatcher == null");
    OperationName operationName = queryWatcher.operation().name();
    registerCall(activeQueryWatchers, operationName, queryWatcher);
  }

  /**
   * <p>Removes provided {@link AppSyncQueryWatcher} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterQueryWatcher(@Nonnull AppSyncQueryWatcher queryWatcher) {
    checkNotNull(queryWatcher, "queryWatcher == null");
    OperationName operationName = queryWatcher.operation().name();
    unregisterCall(activeQueryWatchers, operationName, queryWatcher);
  }

  /**
   * Returns currently active {@link AppSyncQueryWatcher} query watchers by operation name.
   *
   * @param operationName query watcher operation name
   * @return set of active query watchers
   */
  @Nonnull Set<AppSyncQueryWatcher> activeQueryWatchers(@Nonnull OperationName operationName) {
    return activeCalls(activeQueryWatchers, operationName);
  }

  /**
   * Registers idleResourceCallback which is invoked when the apolloClient becomes idle.
   */
  public synchronized void setIdleResourceCallback(IdleResourceCallback idleResourceCallback) {
    this.idleResourceCallback = idleResourceCallback;
  }

  /**
   * Returns a total count of in progress {@link GraphQLCall} & {@link AppSyncPrefetch} objects.
   */
  public int activeCallsCount() {
    return activeCallCount.get();
  }

  private <CALL> void registerCall(Map<OperationName, Set<CALL>> registry, OperationName operationName, CALL call) {
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (registry) {
      Set<CALL> calls = registry.get(operationName);
      if (calls == null) {
        calls = new HashSet<>();
        registry.put(operationName, calls);
      }
      calls.add(call);
    }
    activeCallCount.incrementAndGet();
  }

  private <CALL> void unregisterCall(Map<OperationName, Set<CALL>> registry, OperationName operationName, CALL call) {
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (registry) {
      Set<CALL> calls = registry.get(operationName);
      if (calls == null || !calls.remove(call)) {
        throw new AssertionError("Call wasn't registered before");
      }

      if (calls.isEmpty()) {
        registry.remove(operationName);
      }
    }

    if (activeCallCount.decrementAndGet() == 0) {
      notifyIdleResource();
    }
  }

  private <CALL> Set<CALL> activeCalls(Map<OperationName, Set<CALL>> registry, @Nonnull OperationName operationName) {
    checkNotNull(operationName, "operationName == null");

    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (registry) {
      Set<CALL> calls = registry.get(operationName);
      return calls != null ? new HashSet<>(calls) : Collections.<CALL>emptySet();
    }
  }

  private void notifyIdleResource() {
    IdleResourceCallback callback = idleResourceCallback;
    if (callback != null) {
      callback.onIdle();
    }
  }
}
