/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCache;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * A provider of {@link Record} for reading requests from cache.
 *
 * To serialize a {@link Record} to a standardized form use {@link #recordAdapter()} which handles call custom scalar
 * types registered on the {@link ApolloClient}.
 *
 * If a {@link NormalizedCache} cannot return all the records needed to read a response, it will be considered a cache
 * miss.
 *
 * A {@link NormalizedCache} is recommended to implement support for {@link CacheHeaders} specified in {@link
 * AppSyncCacheHeaders}.
 *
 * A {@link NormalizedCache} can choose to store records in any manner.
 *
 * See {@link LruNormalizedCache} for a in memory cache.
 */
public abstract class NormalizedCache {
  private Optional<NormalizedCache> nextCache = Optional.absent();

  /**
   * @param key          The key of the record to read.
   * @param cacheHeaders The cache headers associated with the request which generated this record.
   * @return The {@link Record} for key. If not present return null.
   */
  @Nullable public abstract Record loadRecord(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders);

  /**
   * Calls through to {@link NormalizedCache#loadRecord(String, CacheHeaders)}. Implementations should override this
   * method if the underlying storage technology can offer an optimized manner to read multiple records.
   *
   * @param keys         The set of {@link Record} keys to read.
   * @param cacheHeaders The cache headers associated with the request which generated this record.
   */
  @Nonnull public Collection<Record> loadRecords(@Nonnull Collection<String> keys, @Nonnull CacheHeaders cacheHeaders) {
    List<Record> records = new ArrayList<>(keys.size());
    for (String key : keys) {
      final Record record = loadRecord(key, cacheHeaders);
      if (record != null) {
        records.add(record);
      }
    }
    return records;
  }

  /**
   * @param record       The {@link Record} to merge.
   * @param cacheHeaders The {@link CacheHeaders} associated with the request which generated this record.
   * @return A set of record field keys that have changed. This set is returned by {@link Record#mergeWith(Record)}.
   */
  @Nonnull public abstract Set<String> merge(@Nonnull Record record, @Nonnull CacheHeaders cacheHeaders);

  /**
   * Calls through to {@link NormalizedCache#merge(Record, CacheHeaders)}. Implementations should override this method
   * if the underlying storage technology can offer an optimized manner to store multiple records.
   *
   * @param recordSet    The set of Records to merge.
   * @param cacheHeaders The {@link CacheHeaders} associated with the request which generated this record.
   * @return A set of record field keys that have changed. This set is returned by {@link Record#mergeWith(Record)}.
   */
  @Nonnull public Set<String> merge(@Nonnull Collection<Record> recordSet, @Nonnull CacheHeaders cacheHeaders) {
    Set<String> aggregatedDependentKeys = new LinkedHashSet<>();
    for (Record record : recordSet) {
      aggregatedDependentKeys.addAll(merge(record, cacheHeaders));
    }
    return aggregatedDependentKeys;
  }

  /**
   * Clears all records from the cache.
   *
   * Clients should call {@link ApolloClient#clearNormalizedCache()} for a thread-safe access to this method.
   */
  public abstract void clearAll();

  /**
   * Remove cached record by the key
   *
   * @param cacheKey of record to be removed
   * @return {@code true} if record with such key was successfully removed, {@code false} otherwise
   */
  public abstract boolean remove(@Nonnull CacheKey cacheKey);

  public final NormalizedCache chain(@Nonnull NormalizedCache cache) {
    checkNotNull(cache, "cache == null");

    NormalizedCache leafCache = this;
    while (leafCache.nextCache.isPresent()) {
      leafCache = leafCache.nextCache.get();
    }
    leafCache.nextCache = Optional.of(cache);

    return this;
  }

  public final Optional<NormalizedCache> nextCache() {
    return nextCache;
  }
}
