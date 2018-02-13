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

package com.apollographql.apollo.cache;

import com.apollographql.apollo.cache.normalized.NormalizedCache;

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
