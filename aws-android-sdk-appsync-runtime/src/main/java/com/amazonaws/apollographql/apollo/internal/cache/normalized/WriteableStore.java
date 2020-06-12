/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.cache.normalized;

import com.amazonaws.apollographql.apollo.cache.CacheHeaders;
import com.amazonaws.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

public interface WriteableStore extends ReadableStore {

  Set<String> merge(@Nonnull Collection<Record> recordCollection, @Nonnull CacheHeaders cacheHeaders);
  Set<String> merge(Record record, @Nonnull CacheHeaders cacheHeaders);
}
