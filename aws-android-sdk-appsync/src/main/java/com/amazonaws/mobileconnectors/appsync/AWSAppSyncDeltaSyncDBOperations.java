
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

final class AWSAppSyncDeltaSyncDBOperations {

    final class DeltaSyncRecord {
        long id;
        String key;
        long lastRunTimeInMilliSeconds;
    }

    private final String[] allColumns = {
            AWSAppSyncDeltaSyncSqlHelper.COLUMN_ID,
            AWSAppSyncDeltaSyncSqlHelper.COLUMN_DELTA_SYNC_KEY,
            AWSAppSyncDeltaSyncSqlHelper.COLUMN_LAST_RUN_TIME,
    };

    private static final String INSERT_STATEMENT =
            String.format("INSERT INTO %S ( %s, %s) VALUES (?,?)",
                    AWSAppSyncDeltaSyncSqlHelper.TABLE_DELTA_SYNC,
                    AWSAppSyncDeltaSyncSqlHelper.COLUMN_DELTA_SYNC_KEY,
                    AWSAppSyncDeltaSyncSqlHelper.COLUMN_LAST_RUN_TIME);

    private static final String DELETE_STATEMENT =
            String.format("DELETE FROM %s WHERE %s = ?",
                    AWSAppSyncDeltaSyncSqlHelper.TABLE_DELTA_SYNC,
                    AWSAppSyncDeltaSyncSqlHelper.COLUMN_ID);

    private static final String UPDATE_LAST_RUN_TIME =
            String.format("UPDATE %s set %s = ? WHERE %s = ?",
                    AWSAppSyncDeltaSyncSqlHelper.TABLE_DELTA_SYNC,
                    AWSAppSyncDeltaSyncSqlHelper.COLUMN_LAST_RUN_TIME,
                    AWSAppSyncDeltaSyncSqlHelper.COLUMN_ID);

    private static final String GET_RECORD_BY_ID =
            String.format("SELECT * FROM %s WHERE %s = ?",
                    AWSAppSyncDeltaSyncSqlHelper.TABLE_DELTA_SYNC,
                    AWSAppSyncDeltaSyncSqlHelper.COLUMN_ID);

    private static final String GET_RECORD_BY_KEY =
            String.format("SELECT * FROM %s WHERE %s = ?",
                    AWSAppSyncDeltaSyncSqlHelper.TABLE_DELTA_SYNC,
                    AWSAppSyncDeltaSyncSqlHelper.COLUMN_DELTA_SYNC_KEY);

    private final SQLiteDatabase database;
    private final SQLiteOpenHelper dbHelper;
    private final SQLiteStatement insertStatement;
    private final SQLiteStatement deleteStatement;
    private final SQLiteStatement updateLastRunTimeStatement;
    private final SQLiteStatement getRecordByID;
    private final SQLiteStatement getRecordByKey;

    AWSAppSyncDeltaSyncDBOperations(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
        database = dbHelper.getWritableDatabase();
        insertStatement = database.compileStatement(INSERT_STATEMENT);
        deleteStatement = database.compileStatement(DELETE_STATEMENT);
        updateLastRunTimeStatement = database.compileStatement(UPDATE_LAST_RUN_TIME);
        getRecordByID = database.compileStatement(GET_RECORD_BY_ID);
        getRecordByKey = database.compileStatement(GET_RECORD_BY_KEY);
    }

    /*
        Create Record for Delta Sync in the Database
     */
    long createRecord(String key, long lastRunTime)
    {
        insertStatement.bindString(1, key);
        insertStatement.bindLong (2, lastRunTime);
        long recordId = insertStatement.executeInsert();
        return recordId;
    }


    /*
        Update the last Run Time in the database
     */
    void updateLastRunTime(long id, long lastRunTime) {
        updateLastRunTimeStatement.bindLong(1, lastRunTime);
        updateLastRunTimeStatement.bindLong(2,id);
        updateLastRunTimeStatement.executeUpdateDelete();
    }

    /*
        delete the deltaSync record from the database
     */
    boolean deleteRecord(Long id) {
        deleteStatement.bindLong(1, id);
        return deleteStatement.executeUpdateDelete() > 0;
    }

    /*
        Get a DeltaSync record using id.
        Will return a AWSAppSyncDeltaSyncDBOperations.DeltaSyncRecord object

     */
    DeltaSyncRecord getRecordByID(long id) {
        DeltaSyncRecord record = null;

        Cursor cursor = database.query(AWSAppSyncDeltaSyncSqlHelper.TABLE_DELTA_SYNC,
                allColumns, AWSAppSyncDeltaSyncSqlHelper.COLUMN_ID + " = ?",
                new String[]{"" + id},
                null,
                null,
                null);

        if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
            record.id = cursor.getLong(cursor.getColumnIndex(AWSAppSyncDeltaSyncSqlHelper.COLUMN_ID));
            record.key = cursor.getString(cursor.getColumnIndex(AWSAppSyncDeltaSyncSqlHelper.COLUMN_DELTA_SYNC_KEY));
            record.lastRunTimeInMilliSeconds = cursor.getLong(cursor.getColumnIndex(AWSAppSyncDeltaSyncSqlHelper.COLUMN_LAST_RUN_TIME));
        }
        cursor.close();
        return record;
    }

    /*
      Get a DeltaSync record using key.
      Will return a AWSAppSyncDeltaSyncDBOperations.DeltaSyncRecord object
   */
    DeltaSyncRecord getRecordByKey(String key) {
        DeltaSyncRecord record = null;

        Cursor cursor = database.query(AWSAppSyncDeltaSyncSqlHelper.TABLE_DELTA_SYNC,
                allColumns, AWSAppSyncDeltaSyncSqlHelper.COLUMN_DELTA_SYNC_KEY + " = ?",
                new String[]{key},
                null,
                null,
                null);

        if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
            record = new DeltaSyncRecord();
            record.id = cursor.getLong(cursor.getColumnIndex(AWSAppSyncDeltaSyncSqlHelper.COLUMN_ID));
            record.key = cursor.getString(cursor.getColumnIndex(AWSAppSyncDeltaSyncSqlHelper.COLUMN_DELTA_SYNC_KEY));
            record.lastRunTimeInMilliSeconds = cursor.getLong(cursor.getColumnIndex(AWSAppSyncDeltaSyncSqlHelper.COLUMN_LAST_RUN_TIME));
        }
        cursor.close();
        return record;
    }

}
