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

import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.api.internal.Optional;

import java.util.concurrent.TimeUnit;

/**
 * Controls how long a {@link Record} will
 * stay in a {@link LruNormalizedCache}.
 */
public final class EvictionPolicy {

  public static final EvictionPolicy NO_EVICTION = EvictionPolicy.builder().build();

  private final Optional<Long> maxSizeBytes;
  private final Optional<Long> maxEntries;
  private final Optional<Long> expireAfterAccess;
  private final Optional<TimeUnit> expireAfterAccessTimeUnit;
  private final Optional<Long> expireAfterWrite;
  private final Optional<TimeUnit> expireAfterWriteTimeUnit;

  Optional<Long> maxSizeBytes() {
    return maxSizeBytes;
  }

  Optional<Long> maxEntries() {
    return maxEntries;
  }

  Optional<Long> expireAfterAccess() {
    return expireAfterAccess;
  }

  Optional<TimeUnit> expireAfterAccessTimeUnit() {
    return expireAfterAccessTimeUnit;
  }

  Optional<Long> expireAfterWrite() {
    return expireAfterWrite;
  }

  Optional<TimeUnit> expireAfterWriteTimeUnit() {
    return expireAfterWriteTimeUnit;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Builder() { }

    private Optional<Long> maxSizeBytes = Optional.absent();
    private Optional<Long> maxEntries = Optional.absent();
    private Optional<Long> expireAfterAccess = Optional.absent();
    private Optional<TimeUnit> expireAfterAccessTimeUnit = Optional.absent();
    private Optional<Long> expireAfterWrite = Optional.absent();
    private Optional<TimeUnit> expireAfterWriteTimeUnit = Optional.absent();

    public Builder maxSizeBytes(long maxSizeBytes) {
      this.maxSizeBytes = Optional.of(maxSizeBytes);
      return this;
    }

    public Builder maxEntries(long maxEntries) {
      this.maxEntries = Optional.of(maxEntries);
      return this;
    }

    public Builder expireAfterAccess(long time, TimeUnit timeUnit) {
      this.expireAfterAccess = Optional.of(time);
      this.expireAfterAccessTimeUnit = Optional.of(timeUnit);
      return this;
    }

    public Builder expireAfterWrite(long time, TimeUnit timeUnit) {
      this.expireAfterWrite = Optional.of(time);
      this.expireAfterWriteTimeUnit = Optional.of(timeUnit);
      return this;
    }

    public EvictionPolicy build() {
      return new EvictionPolicy(maxSizeBytes, maxEntries, expireAfterAccess, expireAfterAccessTimeUnit,
          expireAfterWrite, expireAfterWriteTimeUnit);
    }

  }

  private EvictionPolicy(Optional<Long> maxSizeBytes, Optional<Long> maxEntries, Optional<Long> expireAfterAccess,
      Optional<TimeUnit> expireAfterAccessTimeUnit, Optional<Long> expireAfterWrite, Optional<TimeUnit>
      expireAfterWriteTimeUnit) {
    this.maxSizeBytes = maxSizeBytes;
    this.maxEntries = maxEntries;
    this.expireAfterAccess = expireAfterAccess;
    this.expireAfterAccessTimeUnit = expireAfterAccessTimeUnit;
    this.expireAfterWrite = expireAfterWrite;
    this.expireAfterWriteTimeUnit = expireAfterWriteTimeUnit;
  }

}
