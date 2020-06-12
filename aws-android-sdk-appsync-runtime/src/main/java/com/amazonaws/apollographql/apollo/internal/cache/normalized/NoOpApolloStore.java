/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.cache.normalized;

import com.amazonaws.apollographql.apollo.api.GraphqlFragment;
import com.amazonaws.apollographql.apollo.api.Operation;
import com.amazonaws.apollographql.apollo.api.Response;
import com.amazonaws.apollographql.apollo.api.ResponseFieldMapper;
import com.amazonaws.apollographql.apollo.cache.CacheHeaders;
import com.amazonaws.apollographql.apollo.cache.normalized.ApolloStore;
import com.amazonaws.apollographql.apollo.cache.normalized.GraphQLStoreOperation;
import com.amazonaws.apollographql.apollo.cache.normalized.CacheKey;
import com.amazonaws.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.amazonaws.apollographql.apollo.cache.normalized.NormalizedCache;
import com.amazonaws.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An alternative to {@link RealAppSyncStore} for when a no-operation cache is needed.
 */
public final class NoOpApolloStore implements ApolloStore, ReadableStore, WriteableStore {

  @Override public Set<String> merge(@Nonnull Collection<Record> recordCollection, @Nonnull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Override public Set<String> merge(Record record, @Nonnull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Nullable @Override public Record read(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders) {
    return null;
  }

  @Override public Collection<Record> read(@Nonnull Collection<String> keys, @Nonnull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Override public void subscribe(RecordChangeSubscriber subscriber) {
  }

  @Override public void unsubscribe(RecordChangeSubscriber subscriber) {
  }

  @Override public void publish(Set<String> keys) {
  }

  @Nonnull @Override public GraphQLStoreOperation<Boolean> clearAll() {
    return GraphQLStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override public GraphQLStoreOperation<Boolean> remove(@Nonnull CacheKey cacheKey) {
    return GraphQLStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override public GraphQLStoreOperation<Integer> remove(@Nonnull List<CacheKey> cacheKeys) {
    return GraphQLStoreOperation.emptyOperation(0);
  }

  @Override public ResponseNormalizer<Map<String, Object>> networkResponseNormalizer() {
    //noinspection unchecked
    return ResponseNormalizer.NO_OP_NORMALIZER;
  }

  @Override public ResponseNormalizer<Record> cacheResponseNormalizer() {
    //noinspection unchecked
    return ResponseNormalizer.NO_OP_NORMALIZER;
  }

  @Override public <R> R readTransaction(Transaction<ReadableStore, R> transaction) {
    return transaction.execute(this);
  }

  @Override public <R> R writeTransaction(Transaction<WriteableStore, R> transaction) {
    return transaction.execute(this);
  }

  @Override public NormalizedCache normalizedCache() {
    return null;
  }

  @Override public CacheKeyResolver cacheKeyResolver() {
    return null;
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<T> read(
      @Nonnull Operation<D, T, V> operation) {
    return GraphQLStoreOperation.emptyOperation(null);
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<Response<T>> read(
      @Nonnull Operation<D, T, V> operation, @Nonnull ResponseFieldMapper<D> responseFieldMapper,
      @Nonnull ResponseNormalizer<Record> responseNormalizer, @Nonnull CacheHeaders cacheHeaders) {
    return GraphQLStoreOperation.emptyOperation(Response.<T>builder(operation).build());
  }

  @Nonnull @Override
  public <F extends GraphqlFragment> GraphQLStoreOperation<F> read(@Nonnull ResponseFieldMapper<F> fieldMapper,
                                                                   @Nonnull CacheKey cacheKey, @Nonnull Operation.Variables variables) {
    return GraphQLStoreOperation.emptyOperation(null);
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<Set<String>> write(
      @Nonnull Operation<D, T, V> operation, @Nonnull D operationData) {
    return GraphQLStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<Boolean> writeAndPublish(
      @Nonnull Operation<D, T, V> operation, @Nonnull D operationData) {
    return GraphQLStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override
  public GraphQLStoreOperation<Set<String>> write(@Nonnull GraphqlFragment fragment, @Nonnull CacheKey cacheKey,
                                                  @Nonnull Operation.Variables variables) {
    return GraphQLStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @Nonnull @Override
  public GraphQLStoreOperation<Boolean> writeAndPublish(@Nonnull GraphqlFragment fragment, @Nonnull CacheKey cacheKey,
                                                        @Nonnull Operation.Variables variables) {
    return GraphQLStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<Set<String>>
  writeOptimisticUpdates(@Nonnull Operation<D, T, V> operation, @Nonnull D operationData, @Nonnull UUID mutationId) {
    return GraphQLStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<Boolean>
  writeOptimisticUpdatesAndPublish(@Nonnull Operation<D, T, V> operation, @Nonnull D operationData,
      @Nonnull UUID mutationId) {
    return GraphQLStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override
  public GraphQLStoreOperation<Boolean> rollbackOptimisticUpdatesAndPublish(@Nonnull UUID mutationId) {
    return GraphQLStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override public GraphQLStoreOperation<Set<String>> rollbackOptimisticUpdates(@Nonnull UUID mutationId) {
    return GraphQLStoreOperation.emptyOperation(Collections.<String>emptySet());
  }
}
