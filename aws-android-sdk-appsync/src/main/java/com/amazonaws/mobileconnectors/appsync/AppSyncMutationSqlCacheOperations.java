/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.LinkedList;
import java.util.List;

public final class AppSyncMutationSqlCacheOperations {
    private static final String INSERT_STATEMENT =
            String.format("INSERT INTO %s (%s,%s,%s,%s,%s,%s,%s,%s,%s) VALUES (?,?,?,?,?,?,?,?,?)",
                    AppSyncMutationsSqlHelper.TABLE_MUTATION_RECORDS,
                    AppSyncMutationsSqlHelper.RECORD_IDENTIFIER,
                    AppSyncMutationsSqlHelper.COLUMN_RECORD,
                    AppSyncMutationsSqlHelper.RESPONSE_CLASS,
                    AppSyncMutationsSqlHelper.COLUMN_CLIENT_STATE,
                    AppSyncMutationsSqlHelper.COLUMN_BUCKET,
                    AppSyncMutationsSqlHelper.COLUMN_KEY,
                    AppSyncMutationsSqlHelper.COLUMN_REGION,
                    AppSyncMutationsSqlHelper.COLUMN_LOCAL_URI,
                    AppSyncMutationsSqlHelper.COLUMN_MIME_TYPE);

    private static final String DELETE_STATEMENT =
            String.format("DELETE FROM %s WHERE %s=?",
                    AppSyncMutationsSqlHelper.TABLE_MUTATION_RECORDS,
                    AppSyncMutationsSqlHelper.RECORD_IDENTIFIER);
    private static final String DELETE_ALL_RECORD_STATEMENT = String.format("DELETE FROM %s", AppSyncMutationsSqlHelper.TABLE_MUTATION_RECORDS);
    SQLiteDatabase database;
    private final SQLiteOpenHelper dbHelper;
    private final String[] allColumns = {AppSyncMutationsSqlHelper.COLUMN_ID,
            AppSyncMutationsSqlHelper.RECORD_IDENTIFIER,
            AppSyncMutationsSqlHelper.COLUMN_RECORD,
            AppSyncMutationsSqlHelper.RESPONSE_CLASS,
            AppSyncMutationsSqlHelper.COLUMN_CLIENT_STATE,
            AppSyncMutationsSqlHelper.COLUMN_BUCKET,
            AppSyncMutationsSqlHelper.COLUMN_KEY,
            AppSyncMutationsSqlHelper.COLUMN_REGION,
            AppSyncMutationsSqlHelper.COLUMN_LOCAL_URI,
            AppSyncMutationsSqlHelper.COLUMN_MIME_TYPE};

    private final SQLiteStatement insertStatement;
    private final SQLiteStatement deleteStatement;
    private final SQLiteStatement deleteAllRecordsStatement;

    AppSyncMutationSqlCacheOperations(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
        database = dbHelper.getWritableDatabase();
        insertStatement = database.compileStatement(INSERT_STATEMENT);
        deleteStatement = database.compileStatement(DELETE_STATEMENT);
        deleteAllRecordsStatement = database.compileStatement(DELETE_ALL_RECORD_STATEMENT);
    }

    public void close() {
        dbHelper.close();
    }

    long createRecord(String recordIdentifier, String record, String responseClass,
                      String clientState, String bucket, String key, String region,
                      String localUri, String mimeType) {
        insertStatement.bindString(1, recordIdentifier);
        insertStatement.bindString(2, record);
        insertStatement.bindString(3, responseClass);
        insertStatement.bindString(4, clientState);
        insertStatement.bindString(5, bucket != null ? bucket : "");
        insertStatement.bindString(6, key != null ? key : "");
        insertStatement.bindString(7, region != null ? region : "");
        insertStatement.bindString(8, localUri != null ? localUri : "");
        insertStatement.bindString(9, mimeType != null ? mimeType : "");
        long recordId = insertStatement.executeInsert();
        return recordId;
    }

    boolean deleteRecord(String recordIdentifier) {
        deleteStatement.bindString(1, recordIdentifier);
        return deleteStatement.executeUpdateDelete() > 0;
    }

    List<PersistentOfflineMutationObject> fetchAllRecords() {
        LinkedList<PersistentOfflineMutationObject> mutationObjects = new LinkedList<>();

        Cursor cursor = null;

        try {
            cursor = database.query(AppSyncMutationsSqlHelper.TABLE_MUTATION_RECORDS,
                    allColumns, null, null,
                    null, null, AppSyncMutationsSqlHelper.COLUMN_ID);

            if (cursor == null || !cursor.moveToFirst()) {
                return mutationObjects;
            }

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {

                    String recordIdentifier = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.RECORD_IDENTIFIER));
                    String record = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.COLUMN_RECORD));
                    String responseClass = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.RESPONSE_CLASS));
                    String clientState = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.COLUMN_CLIENT_STATE));
                    String bucket = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.COLUMN_BUCKET));
                    String key = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.COLUMN_KEY));
                    String region = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.COLUMN_REGION));
                    String localUri = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.COLUMN_LOCAL_URI));
                    String mimeType = cursor.getString(cursor.getColumnIndex(AppSyncMutationsSqlHelper.COLUMN_MIME_TYPE));
                    PersistentOfflineMutationObject mutationObject = new PersistentOfflineMutationObject(recordIdentifier,
                            record,
                            responseClass,
                            clientState,
                            bucket,
                            key,
                            region,
                            localUri,
                            mimeType);
                    mutationObjects.add(mutationObject);
                    cursor.moveToNext();
                }
            }

        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return mutationObjects;
    }

    void clearCurrentCache() {
        deleteAllRecordsStatement.execute();
    }
}
