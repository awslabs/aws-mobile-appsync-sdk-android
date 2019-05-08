/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
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

    private static final String DATABASE_NAME = "appsync_deltasync_db";
    private static final int DATABASE_VERSION = 1;

    //Database Create Statement
    private static final String DATABASE_CREATE = String.format(
            "create table %s( %s integer primary key autoincrement, %s text not null, %s Integer);",
            TABLE_DELTA_SYNC,
            COLUMN_ID,
            COLUMN_DELTA_SYNC_KEY,
            COLUMN_LAST_RUN_TIME);


    //Constructor
    public AWSAppSyncDeltaSyncSqlHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //Constructor
    public AWSAppSyncDeltaSyncSqlHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
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
