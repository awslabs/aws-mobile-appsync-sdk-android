/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.api.cache.http;

import java.io.IOException;

import javax.annotation.Nonnull;

import okio.Sink;

public interface HttpCacheRecordEditor {
  @Nonnull Sink headerSink();

  @Nonnull Sink bodySink();

  void abort() throws IOException;

  void commit() throws IOException;
}
