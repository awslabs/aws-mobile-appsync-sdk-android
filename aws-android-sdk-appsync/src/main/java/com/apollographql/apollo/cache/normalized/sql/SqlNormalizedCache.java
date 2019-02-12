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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.amazonaws.mobileconnectors.appsync.cache.normalized.sql.AppSyncSqlHelper;
import com.apollographql.apollo.api.internal.Action;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.cache.GraphQLCacheHeaders.DO_NOT_STORE;
import static com.apollographql.apollo.cache.GraphQLCacheHeaders.EVICT_AFTER_READ;

public final class SqlNormalizedCache extends NormalizedCache {
  private static final String INSERT_STATEMENT =
      String.format("INSERT INTO %s (%s,%s) VALUES (?,?)",
          AppSyncSqlHelper.TABLE_RECORDS,
          AppSyncSqlHelper.COLUMN_KEY,
          AppSyncSqlHelper.COLUMN_RECORD);
  private static final String UPDATE_STATEMENT =
      String.format("UPDATE %s SET %s=?, %s=? WHERE %s=?",
          AppSyncSqlHelper.TABLE_RECORDS,
          AppSyncSqlHelper.COLUMN_KEY,
          AppSyncSqlHelper.COLUMN_RECORD,
          AppSyncSqlHelper.COLUMN_KEY);
  private static final String DELETE_STATEMENT =
      String.format("DELETE FROM %s WHERE %s=?",
          AppSyncSqlHelper.TABLE_RECORDS,
          AppSyncSqlHelper.COLUMN_KEY);
  private static final String DELETE_ALL_RECORD_STATEMENT = String.format("DELETE FROM %s", AppSyncSqlHelper.TABLE_RECORDS);
  SQLiteDatabase database;
  private final SQLiteOpenHelper dbHelper;
  private final String[] allColumns = {AppSyncSqlHelper.COLUMN_ID,
      AppSyncSqlHelper.COLUMN_KEY,
      AppSyncSqlHelper.COLUMN_RECORD};

  private final SQLiteStatement insertStatement;
  private final SQLiteStatement updateStatement;
  private final SQLiteStatement deleteStatement;
  private final SQLiteStatement deleteAllRecordsStatement;
  private final RecordFieldJsonAdapter recordFieldAdapter;

  SqlNormalizedCache(RecordFieldJsonAdapter recordFieldAdapter, SQLiteOpenHelper dbHelper) {
    this.recordFieldAdapter = recordFieldAdapter;
    this.dbHelper = dbHelper;
    database = dbHelper.getWritableDatabase();
    insertStatement = database.compileStatement(INSERT_STATEMENT);
    updateStatement = database.compileStatement(UPDATE_STATEMENT);
    deleteStatement = database.compileStatement(DELETE_STATEMENT);
    deleteAllRecordsStatement = database.compileStatement(DELETE_ALL_RECORD_STATEMENT);
  }

  @Nullable public Record loadRecord(@Nonnull final String key, @Nonnull final CacheHeaders cacheHeaders) {
    return selectRecordForKey(key)
        .apply(new Action<Record>() {
          @Override
          public void apply(@Nonnull Record record) {
            if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
              deleteRecord(key);
            }
          }
        })
        .or(nextCache().flatMap(new Function<NormalizedCache, Optional<Record>>() {
          @Nonnull @Override
          public Optional<Record> apply(@Nonnull NormalizedCache cache) {
            return Optional.fromNullable(cache.loadRecord(key, cacheHeaders));
          }
        }))
        .orNull();
  }

  @Nonnull public Set<String> merge(@Nonnull final Record apolloRecord, @Nonnull final CacheHeaders cacheHeaders) {
    if (cacheHeaders.hasHeader(DO_NOT_STORE)) {
      return Collections.emptySet();
    }

    //noinspection ResultOfMethodCallIgnored
    Optional<NormalizedCache> normalizedCacheOptional = nextCache().apply(new Action<NormalizedCache>() {
      @Override
      public void apply(@Nonnull NormalizedCache cache) {
        cache.merge(apolloRecord, cacheHeaders);
      }
    });

    Optional<Record> optionalOldRecord = selectRecordForKey(apolloRecord.key());
    Set<String> changedKeys;
    if (!optionalOldRecord.isPresent()) {
      createRecord(apolloRecord.key(), recordFieldAdapter.toJson(apolloRecord.fields()));
      changedKeys = Collections.emptySet();
    } else {
      Record oldRecord = optionalOldRecord.get();
      changedKeys = oldRecord.mergeWith(apolloRecord);
      if (!changedKeys.isEmpty()) {
        updateRecord(oldRecord.key(), recordFieldAdapter.toJson(oldRecord.fields()));
      }
    }

    return changedKeys;
  }

  @Nonnull @Override
  public Set<String> merge(@Nonnull final Collection<Record> recordSet,
                           @Nonnull final CacheHeaders cacheHeaders) {
    if (cacheHeaders.hasHeader(DO_NOT_STORE)) {
      return Collections.emptySet();
    }

    //noinspection ResultOfMethodCallIgnored
    Optional<NormalizedCache> normalizedCacheOptional = nextCache().apply(new Action<NormalizedCache>() {
      @Override
      public void apply(@Nonnull NormalizedCache cache) {
        for (Record record : recordSet) {
          cache.merge(record, cacheHeaders);
        }
      }
    });

    Set<String> changedKeys;
    try {
      database.beginTransaction();
      changedKeys = super.merge(recordSet, cacheHeaders);
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
    return changedKeys;
  }

  @Override
  public void clearAll() {
    //noinspection ResultOfMethodCallIgnored
    Optional<NormalizedCache> normalizedCacheOptional = nextCache().apply(new Action<NormalizedCache>() {
      @Override
      public void apply(@Nonnull NormalizedCache cache) {
        cache.clearAll();
      }
    });
    clearCurrentCache();
  }

  @Override
  public boolean remove(@Nonnull final CacheKey cacheKey) {
    checkNotNull(cacheKey, "cacheKey == null");
    boolean result;

    result = nextCache().map(new Function<NormalizedCache, Boolean>() {
      @Nonnull @Override
      public Boolean apply(@Nonnull NormalizedCache cache) {
        return cache.remove(cacheKey);
      }
    }).or(Boolean.FALSE);

    return result | deleteRecord(cacheKey.key());
  }

  public void close() {
    dbHelper.close();
  }

  long createRecord(String key, String fields) {
    insertStatement.bindString(1, key);
    insertStatement.bindString(2, fields);

    long recordId = insertStatement.executeInsert();
    return recordId;
  }

  void updateRecord(String key, String fields) {
    updateStatement.bindString(1, key);
    updateStatement.bindString(2, fields);
    updateStatement.bindString(3, key);

    updateStatement.executeInsert();
  }

  boolean deleteRecord(String key) {
    deleteStatement.bindString(1, key);
    return deleteStatement.executeUpdateDelete() > 0;
  }

  Optional<Record> selectRecordForKey(String key) {
    Cursor cursor = database.query(AppSyncSqlHelper.TABLE_RECORDS,
        allColumns, AppSyncSqlHelper.COLUMN_KEY + " = ?", new String[]{key},
        null, null, null);
    if (cursor == null || !cursor.moveToFirst()) {
      return Optional.absent();
    }
    try {
      return Optional.of(cursorToRecord(cursor));
    } catch (IOException exception) {
      return Optional.absent();
    } finally {
      cursor.close();
    }
  }

  Record cursorToRecord(Cursor cursor) throws IOException {
    String key = cursor.getString(1);
    String jsonOfFields = cursor.getString(2);
    return Record.builder(key).addFields(recordFieldAdapter.from(jsonOfFields)).build();
  }

  void clearCurrentCache() {
    deleteAllRecordsStatement.execute();
  }
}
