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

package com.apollographql.apollo.cache;

import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A key/value collection which is sent with {@link Record}
 * from a {@link com.apollographql.apollo.api.Operation} to the
 * {@link NormalizedCache}.
 *
 * For headers which the default {@link NormalizedCache} respect, see
 * {@link GraphQLCacheHeaders}.
 */
public final class CacheHeaders {

  private final Map<String, String> headerMap;

  public static Builder builder() {
    return new Builder();
  }

  public static final CacheHeaders NONE = new CacheHeaders(Collections.<String, String>emptyMap());

  public static final class Builder {

    private final Map<String, String> headerMap = new LinkedHashMap<>();

    public Builder addHeader(String headerName, String headerValue) {
      headerMap.put(headerName, headerValue);
      return this;
    }

    public Builder addHeaders(Map<String, String> headerMap) {
      this.headerMap.putAll(headerMap);
      return this;
    }

    public CacheHeaders build() {
      return new CacheHeaders(headerMap);
    }
  }

  /**
   * @return A {@link Builder} with a copy of this {@link CacheHeaders} values.
   */
  public Builder toBuilder() {
    Builder builder = builder();
    builder.addHeaders(headerMap);
    return builder;
  }

  private CacheHeaders(Map<String, String> headerMap) {
    this.headerMap = headerMap;
  }

  @Nullable
  public String headerValue(String header) {
    return headerMap.get(header);
  }

  public boolean hasHeader(String headerName) {
    return headerMap.containsKey(headerName);
  }
}
