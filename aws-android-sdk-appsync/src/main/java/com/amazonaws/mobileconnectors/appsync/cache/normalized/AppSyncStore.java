/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.cache.normalized;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.GraphQLStoreOperation;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.cache.normalized.ReadableStore;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.cache.normalized.Transaction;
import com.apollographql.apollo.internal.cache.normalized.WriteableStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class AppSyncStore {

    private ApolloStore mStore;

    public AppSyncStore(ApolloStore syncStore) {
        mStore = syncStore;
    }

    public ResponseNormalizer<Map<String, Object>> networkResponseNormalizer() {
        return mStore.networkResponseNormalizer();
    }

    public ResponseNormalizer<Record> cacheResponseNormalizer() {
        return mStore.cacheResponseNormalizer();
    }

    public synchronized void subscribe(ApolloStore.RecordChangeSubscriber subscriber) {
        mStore.subscribe(subscriber);
    }

    public synchronized void unsubscribe(ApolloStore.RecordChangeSubscriber subscriber) {
        mStore.unsubscribe(subscriber);
    }

    public void publish(@Nonnull final Set<String> changedKeys) {
        mStore.publish(changedKeys);
    }

    public GraphQLStoreOperation<Boolean> clearAll() {
        return mStore.clearAll();
    }

    public GraphQLStoreOperation<Boolean> remove(@Nonnull final CacheKey cacheKey) {
        return mStore.remove(cacheKey);
    }

    public GraphQLStoreOperation<Integer> remove(@Nonnull final List<CacheKey> cacheKeys) {
        return mStore.remove(cacheKeys);
    }

    public <R> R readTransaction(Transaction<ReadableStore, R> transaction) {
        return mStore.readTransaction(transaction);
    }

    public <R> R writeTransaction(Transaction<WriteableStore, R> transaction) {
        return mStore.writeTransaction(transaction);
    }

    public NormalizedCache normalizedCache() {
        return mStore.normalizedCache();
    }

    public Record read(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders) {
        return ((ReadableStore) mStore).read(key, cacheHeaders);
    }

    public Collection<Record> read(@Nonnull Collection<String> keys, @Nonnull CacheHeaders cacheHeaders) {
        return ((ReadableStore) mStore).read(keys, cacheHeaders);
    }

    public Set<String> merge(@Nonnull Collection<Record> recordSet, @Nonnull CacheHeaders cacheHeaders) {
        return ((WriteableStore) mStore).merge(recordSet, cacheHeaders);
    }

    public Set<String> merge(Record record, @Nonnull CacheHeaders cacheHeaders) {
        return ((WriteableStore) mStore).merge(record, cacheHeaders);
    }

    public CacheKeyResolver cacheKeyResolver() {
        return mStore.cacheKeyResolver();
    }

    public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<T> read(
            @Nonnull final Operation<D, T, V> operation) {
        return mStore.read(operation);
    }

    public <D extends Operation.Data, T, V extends Operation.Variables>
    GraphQLStoreOperation<Response<T>> read(@Nonnull final Operation<D, T, V> operation,
                                            @Nonnull final ResponseFieldMapper<D> responseFieldMapper,
                                            @Nonnull final ResponseNormalizer<Record> responseNormalizer, @Nonnull final CacheHeaders cacheHeaders) {
        return mStore.read(operation, responseFieldMapper, responseNormalizer, cacheHeaders);
    }

    public <F extends GraphqlFragment> GraphQLStoreOperation<F> read(
            @Nonnull final ResponseFieldMapper<F> responseFieldMapper, @Nonnull final CacheKey cacheKey,
            @Nonnull final Operation.Variables variables) {
        return mStore.read(responseFieldMapper, cacheKey, variables);
    }

    public <D extends Operation.Data, T, V extends Operation.Variables>
    GraphQLStoreOperation<Set<String>> write(@Nonnull final Operation<D, T, V> operation, @Nonnull final D operationData) {
        return mStore.write(operation, operationData);
    }

    public <D extends Operation.Data, T, V extends Operation.Variables> GraphQLStoreOperation<Boolean>
    writeAndPublish(@Nonnull final Operation<D, T, V> operation, @Nonnull final D operationData) {
        return mStore.writeAndPublish(operation, operationData);
    }

    public GraphQLStoreOperation<Set<String>> write(@Nonnull final GraphqlFragment fragment,
                                                    @Nonnull final CacheKey cacheKey, @Nonnull final Operation.Variables variables) {
        return mStore.write(fragment, cacheKey, variables);
    }

    public GraphQLStoreOperation<Boolean> writeAndPublish(@Nonnull final GraphqlFragment fragment,
                                                          @Nonnull final CacheKey cacheKey, @Nonnull final Operation.Variables variables) {
        return mStore.writeAndPublish(fragment, cacheKey, variables);
    }

}
