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

package com.amazonaws.mobileconnectors.appsync.cache.normalized.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class AppSyncSqlHelper extends SQLiteOpenHelper {

  public static final String TABLE_RECORDS = "records";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_RECORD = "record";
  public static final String COLUMN_KEY = "key";

  private static final String DATABASE_NAME = "appsync.db";
  private static final int DATABASE_VERSION = 1;
  private static final String TAG = AppSyncSqlHelper.class.getSimpleName();

  // Database creation sql statement
  private static final String DATABASE_CREATE = String.format(
      "create table %s( %s integer primary key autoincrement, %s text not null, %s text not null);",
      TABLE_RECORDS, COLUMN_ID, COLUMN_KEY, COLUMN_RECORD);

  public static final String IDX_RECORDS_KEY = "idx_records_key";
  private static final String CREATE_KEY_INDEX =
      String.format("CREATE INDEX %s ON %s (%s)", IDX_RECORDS_KEY, TABLE_RECORDS, COLUMN_KEY);

  private AppSyncSqlHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public AppSyncSqlHelper(Context context, String name) {
    super(context, name, null, DATABASE_VERSION);
  }

  public static AppSyncSqlHelper create(Context context) {
    return new AppSyncSqlHelper(context);
  }

  public static AppSyncSqlHelper create(Context context, String name) {
    return new AppSyncSqlHelper(context, name);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    Log.d(TAG, "In onCreate for [" + DATABASE_NAME + "]. Will create tables now");
    database.execSQL(DATABASE_CREATE);
    database.execSQL(CREATE_KEY_INDEX);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.d(TAG, "In onUpgrade for [" + DATABASE_NAME + "]. Will drop tables now");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
    onCreate(db);
  }
}
