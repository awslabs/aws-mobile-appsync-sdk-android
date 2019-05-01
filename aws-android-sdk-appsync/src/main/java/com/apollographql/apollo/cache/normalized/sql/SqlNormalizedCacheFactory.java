/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.cache.normalized.sql;

import com.amazonaws.mobileconnectors.appsync.cache.normalized.sql.AppSyncSqlHelper;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class SqlNormalizedCacheFactory extends NormalizedCacheFactory<SqlNormalizedCache> {
  private final AppSyncSqlHelper helper;

  public SqlNormalizedCacheFactory(AppSyncSqlHelper helper) {
    this.helper = checkNotNull(helper, "helper == null");
  }

  @Override
  public SqlNormalizedCache create(RecordFieldJsonAdapter recordFieldAdapter) {
    return new SqlNormalizedCache(recordFieldAdapter, helper);
  }
}
