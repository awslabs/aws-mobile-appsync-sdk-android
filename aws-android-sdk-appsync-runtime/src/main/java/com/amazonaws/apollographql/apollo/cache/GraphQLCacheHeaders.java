/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.cache;

import com.amazonaws.apollographql.apollo.cache.normalized.NormalizedCache;

/**
 * A collection of cache headers that Apollo's implementations of {@link NormalizedCache} respect.
 */
public final class GraphQLCacheHeaders {

  /**
   * Records from this request should not be stored in the {@link NormalizedCache}.
   */
  public static final String DO_NOT_STORE = "do-not-store";

  /**
   * Records from this request should be evicted after being read.
   */
  public static final String EVICT_AFTER_READ = "evict-after-read";
}
