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
