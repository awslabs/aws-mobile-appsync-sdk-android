/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.cache.normalized;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * A key for a {@link Record} used for normalization in a {@link com.apollographql.apollo.cache.normalized.NormalizedCache}.
 * If the json object which the {@link Record} corresponds to does not have a suitable
 * key, return use {@link #NO_KEY}.
 */
public final class CacheKey {

  public static final CacheKey NO_KEY = new CacheKey("");

  public static CacheKey from(@Nonnull String key) {
    return new CacheKey(checkNotNull(key, "key == null"));
  }

  private final String key;

  private CacheKey(@Nonnull String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CacheKey)) return false;

    CacheKey cacheKey = (CacheKey) o;

    return key.equals(cacheKey.key);
  }

  @Override public int hashCode() {
    return key.hashCode();
  }

  @Override public String toString() {
    return key;
  }
}
