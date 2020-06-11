/*
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DATABASE_NAME_DELIMITER;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_MUTATION_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_QUERY_SQL_STORE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This utility must live in the same package as {@link AWSAppSyncClient},
 * since it accesses package-visibility fields on the {@link AWSAppSyncClient}.
 */
public final class SyncStore {
    private SyncStore() {}

    public static void validate(AWSAppSyncClient client, String clientDatabasePrefix) {
        assertNotNull(client.mSyncStore);
        if (clientDatabasePrefix != null) {
            assertEquals(
                clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_QUERY_SQL_STORE_NAME,
                client.querySqlStoreName
            );
            assertEquals(
                clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_MUTATION_SQL_STORE_NAME,
                client.mutationSqlStoreName
            );
            assertEquals(
                clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_DELTA_SYNC_SQL_STORE_NAME,
                client.deltaSyncSqlStoreName
            );
        } else {
            assertEquals(DEFAULT_QUERY_SQL_STORE_NAME, client.querySqlStoreName);
            assertEquals(DEFAULT_MUTATION_SQL_STORE_NAME, client.mutationSqlStoreName);
            assertEquals(DEFAULT_DELTA_SYNC_SQL_STORE_NAME, client.deltaSyncSqlStoreName);
        }
    }
}
