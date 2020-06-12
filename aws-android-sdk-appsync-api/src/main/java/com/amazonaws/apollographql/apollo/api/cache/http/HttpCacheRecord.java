/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api.cache.http;

import javax.annotation.Nonnull;

import okio.Source;

public interface HttpCacheRecord {
  @Nonnull Source headerSource();

  @Nonnull Source bodySource();

  void close();
}
