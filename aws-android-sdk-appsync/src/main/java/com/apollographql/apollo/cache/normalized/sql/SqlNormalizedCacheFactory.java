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
