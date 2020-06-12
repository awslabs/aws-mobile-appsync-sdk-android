/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.cache.normalized;

import com.amazonaws.apollographql.apollo.api.ResponseField;
import com.amazonaws.apollographql.apollo.api.Mutation;
import com.amazonaws.apollographql.apollo.api.Operation;
import com.amazonaws.apollographql.apollo.api.Query;
import com.amazonaws.apollographql.apollo.api.Subscription;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Resolves a cache key for a JSON object.
 */
public abstract class CacheKeyResolver {
  public static final CacheKeyResolver DEFAULT = new CacheKeyResolver() {
    @Nonnull @Override
    public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
      return CacheKey.NO_KEY;
    }

    @Nonnull @Override
    public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {
      return CacheKey.NO_KEY;
    }
  };
  public static final CacheKey QUERY_ROOT_KEY = CacheKey.from("QUERY_ROOT");
  public static final CacheKey MUTATION_ROOT_KEY = CacheKey.from("MUTATION_ROOT");
  public static final CacheKey SUBSCRIPTION_ROOT_KEY = CacheKey.from("SUBSCRIPTION_ROOT");

  public static CacheKey rootKeyForOperation(@Nonnull Operation operation) {
    if (operation instanceof Query) {
      return QUERY_ROOT_KEY;
    } else if (operation instanceof Mutation) {
      return MUTATION_ROOT_KEY;
    } else if (operation instanceof Subscription) {
      return SUBSCRIPTION_ROOT_KEY;
    }
    throw new IllegalArgumentException("Unknown operation type.");
  }

  @Nonnull public abstract CacheKey fromFieldRecordSet(@Nonnull ResponseField field,
      @Nonnull Map<String, Object> recordSet);

  @Nonnull public abstract CacheKey fromFieldArguments(@Nonnull ResponseField field,
      @Nonnull Operation.Variables variables);
}
