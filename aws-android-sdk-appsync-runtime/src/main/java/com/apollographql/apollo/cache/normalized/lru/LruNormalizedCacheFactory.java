/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.cache.normalized.lru;

import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class LruNormalizedCacheFactory extends NormalizedCacheFactory<LruNormalizedCache> {
  private final EvictionPolicy evictionPolicy;

  /**
   * @param evictionPolicy {@link EvictionPolicy} to manage the primary cache.
   */
  public LruNormalizedCacheFactory(EvictionPolicy evictionPolicy) {
    this.evictionPolicy = checkNotNull(evictionPolicy, "evictionPolicy == null");
  }

  @Override public LruNormalizedCache create(final RecordFieldJsonAdapter fieldAdapter) {
    return new LruNormalizedCache(evictionPolicy);
  }
}
