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

public class AppSyncMutationsSqlHelper extends SQLiteOpenHelper {

    public static final String TABLE_MUTATION_RECORDS = "mutation_records";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_RECORD = "record";
    public static final String RECORD_IDENTIFIER = "record_id";
    public static final String RESPONSE_CLASS = "response_class";
    public static final String COLUMN_BUCKET = "bucket";
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_REGION = "region";
    public static final String COLUMN_LOCAL_URI = "local_uri";
    public static final String COLUMN_MIME_TYPE= "mime_type";
    public static final String COLUMN_CLIENT_STATE = "client_state";

    private static final String DATABASE_NAME = "appsync.mutations.db";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = String.format(
            "create table %s( %s integer primary key autoincrement, %s text not null, %s text not null, %s text not null," +
                    " %s text not null, %s text, %s text, %s text, %s text, %s text);",
            TABLE_MUTATION_RECORDS, COLUMN_ID, RECORD_IDENTIFIER, COLUMN_RECORD, RESPONSE_CLASS, COLUMN_CLIENT_STATE,
            COLUMN_BUCKET, COLUMN_KEY, COLUMN_REGION, COLUMN_LOCAL_URI, COLUMN_MIME_TYPE);

    public static final String IDX_RECORDS_KEY = "idx_records_key";
    private static final String CREATE_KEY_INDEX =
            String.format("CREATE INDEX %s ON %s (%s)", IDX_RECORDS_KEY, TABLE_MUTATION_RECORDS, RECORD_IDENTIFIER);

    private AppSyncMutationsSqlHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public AppSyncMutationsSqlHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    public static AppSyncMutationsSqlHelper create(Context context) {
        return new AppSyncMutationsSqlHelper(context);
    }

    public static AppSyncMutationsSqlHelper create(Context context, String name) {
        return new AppSyncMutationsSqlHelper(context, name);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(CREATE_KEY_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MUTATION_RECORDS);
        onCreate(db);
    }
}
