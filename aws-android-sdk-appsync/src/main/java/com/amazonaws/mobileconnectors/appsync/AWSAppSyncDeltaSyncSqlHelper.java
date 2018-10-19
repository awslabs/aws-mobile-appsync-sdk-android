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


package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*
    Delta Sync Database Helper
 */
class AWSAppSyncDeltaSyncSqlHelper extends SQLiteOpenHelper {

    public static final String TABLE_DELTA_SYNC = "delta_sync";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DELTA_SYNC_KEY = "delta_sync_key";
    public static final String COLUMN_LAST_RUN_TIME = "last_run_time";
    public static final String COLUMN_BASE_REFRESH_INTERVAL_IN_SECONDS = "base_refresh_interval_in_seconds";

    private static final String DATABASE_NAME = "appsync_deltasync_db";
    private static final int DATABASE_VERSION = 1;

    //Database Create Statement
    private static final String DATABASE_CREATE = String.format(
            "create table %s( %s integer primary key autoincrement, %s text not null, %s Integer, %s Integer);",
            TABLE_DELTA_SYNC,
            COLUMN_ID,
            COLUMN_DELTA_SYNC_KEY,
            COLUMN_LAST_RUN_TIME,
            COLUMN_BASE_REFRESH_INTERVAL_IN_SECONDS);


    //Constructor
    public AWSAppSyncDeltaSyncSqlHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    /*
        Create the database
     */
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    /*
        Drop the table
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DELTA_SYNC);
        onCreate(db);
    }
}
