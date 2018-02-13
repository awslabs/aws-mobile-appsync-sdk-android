/**
 * Copyright 2018-2018 Amazon.com,
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
