/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.apollographql.apollo.api.S3ObjectManager;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DATABASE_NAME_DELIMITER;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_MUTATION_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_QUERY_SQL_STORE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

final class AWSAppSyncClients {
    private static final String TAG = AWSAppSyncClients.class.getName();

    @NonNull
    static AWSAppSyncClient withApiKeyForGogiTest() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("SubscriptionIntegrationTestGogi");
        return AWSAppSyncClient.builder()
            .context(getTargetContext())
            .awsConfiguration(awsConfiguration)
            .build();
    }

    @NonNull
    static AWSAppSyncClient withIAMFromAWSConfiguration() {
        return withIAMFromAWSConfiguration(true, 0);
    }

    @NonNull
    static AWSAppSyncClient withIAMFromAWSConfiguration(boolean subscriptionsAutoReconnect, long credentialsDelay) {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AWS_IAM");

        JSONObject ccpConfig = awsConfiguration.optJsonObject("CredentialsProvider");
        String cognitoIdentityPoolID = JsonExtract.stringValue(ccpConfig, "CognitoIdentity.Default.PoolId");
        String cognitoRegion = JsonExtract.stringValue(ccpConfig,"CognitoIdentity.Default.Region");

        DelayedCognitoCredentialsProvider credentialsProvider =
            new DelayedCognitoCredentialsProvider(
                cognitoIdentityPoolID,
                Regions.fromName(cognitoRegion),
                credentialsDelay
            );

        AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider, Region.getRegion("us-west-2"));
        S3ObjectManager s3ObjectManager = new S3ObjectManagerImplementation(s3Client);

        return AWSAppSyncClient.builder()
            .context(getTargetContext())
            .credentialsProvider(credentialsProvider)
            .awsConfiguration(awsConfiguration)
            .conflictResolver(new TestConflictResolver())
            .s3ObjectManager(s3ObjectManager)
            .subscriptionsAutoReconnect(subscriptionsAutoReconnect)
            .mutationQueueExecutionTimeout(TimeUnit.SECONDS.toMillis(30))
            .persistentMutationsCallback(LoggingPersistentMutationsCallback.instance())
            .useClientDatabasePrefix(true)
            .build();
    }

    @NonNull
    static AWSAppSyncClient withAPIKEYFromAWSConfiguration() {
        return withAPIKEYFromAWSConfiguration(true, 0);
    }

    @NonNull
    static AWSAppSyncClient withAPIKEYFromAWSConfiguration(boolean subscriptionsAutoReconnect, long credentialsDelay) {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());

        JSONObject ccpConfig = awsConfiguration.optJsonObject("CredentialsProvider");
        String cognitoIdentityPoolID = JsonExtract.stringValue(ccpConfig, "CognitoIdentity.Default.PoolId");
        String cognitoRegion = JsonExtract.stringValue(ccpConfig, "CognitoIdentity.Default.Region");

        DelayedCognitoCredentialsProvider credentialsProvider =
            new DelayedCognitoCredentialsProvider(
                cognitoIdentityPoolID,
                Regions.fromName(cognitoRegion),
                credentialsDelay
            );

        AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider, Region.getRegion("us-west-2"));
        S3ObjectManager s3ObjectManager = new S3ObjectManagerImplementation((s3Client));

        return AWSAppSyncClient.builder()
            .context(getTargetContext())
            .awsConfiguration(awsConfiguration)
            .conflictResolver(new TestConflictResolver())
            .s3ObjectManager(s3ObjectManager)
            .subscriptionsAutoReconnect(subscriptionsAutoReconnect)
            .mutationQueueExecutionTimeout(TimeUnit.SECONDS.toMillis(30))
            .persistentMutationsCallback(LoggingPersistentMutationsCallback.instance())
            .useClientDatabasePrefix(true)
            .build();
    }

    @NonNull
    static AWSAppSyncClient withUserPoolsFromAWSConfiguration() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS");

        return AWSAppSyncClient.builder()
            .context(getTargetContext())
            .awsConfiguration(awsConfiguration)
            .cognitoUserPoolsAuthProvider(() -> {
                try {
                    String idToken = TestAWSMobileClient.instance(getTargetContext())
                        .getTokens()
                        .getIdToken()
                        .getTokenString();
                    Log.d(TAG, "idToken = " + idToken);
                    return idToken;
                } catch (Exception failure) {
                    failure.printStackTrace();
                    return null;
                }
            })
            .useClientDatabasePrefix(true)
            .build();
    }

    @NonNull
    static AWSAppSyncClient withUserPools2FromAWSConfiguration(
            String idTokenStringForCustomCognitoUserPool) {
        // Amazon Cognito User Pools - Custom CognitoUserPool
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS_2");

        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        String clientDatabasePrefix = JsonExtract.stringValue(appSyncConfig, "ClientDatabasePrefix");
        String clientName = JsonExtract.stringValue(appSyncConfig, "AuthMode");

        AWSAppSyncClient.Builder awsAppSyncClientBuilder4 = AWSAppSyncClient.builder()
            .context(getTargetContext())
            .cognitoUserPoolsAuthProvider(() -> idTokenStringForCustomCognitoUserPool)
            .awsConfiguration(awsConfiguration)
            .useClientDatabasePrefix(true);

        AWSAppSyncClient awsAppSyncClient4 = awsAppSyncClientBuilder4.build();
        assertAWSAppSynClientObjectConstruction(awsAppSyncClient4, clientDatabasePrefix, clientName);
        return awsAppSyncClient4;
    }

    static void assertAWSAppSynClientObjectConstruction(
            AWSAppSyncClient awsAppSyncClient, String clientDatabasePrefix, @SuppressWarnings("unused") String clientName) {
        assertNotNull(awsAppSyncClient);
        assertNotNull(awsAppSyncClient.mSyncStore);
        if (clientDatabasePrefix != null) {
            assertEquals(
                clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_QUERY_SQL_STORE_NAME,
                awsAppSyncClient.querySqlStoreName
            );
            assertEquals(
                clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_MUTATION_SQL_STORE_NAME,
                awsAppSyncClient.mutationSqlStoreName
            );
            assertEquals(
                clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_DELTA_SYNC_SQL_STORE_NAME,
                awsAppSyncClient.deltaSyncSqlStoreName
            );
        } else {
            assertEquals(DEFAULT_QUERY_SQL_STORE_NAME, awsAppSyncClient.querySqlStoreName);
            assertEquals(DEFAULT_MUTATION_SQL_STORE_NAME, awsAppSyncClient.mutationSqlStoreName);
            assertEquals(DEFAULT_DELTA_SYNC_SQL_STORE_NAME, awsAppSyncClient.deltaSyncSqlStoreName);
        }
    }

    @NonNull
    static AWSAppSyncClient withUserPoolsFromAWSConfiguration(ResponseFetcher responseFetcher) {
        // Amazon Cognito User Pools
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS");

        return AWSAppSyncClient.builder()
            .context(getTargetContext())
            .awsConfiguration(awsConfiguration)
            .cognitoUserPoolsAuthProvider(() -> {
                try {
                    String idToken = TestAWSMobileClient.instance(getTargetContext())
                        .getTokens()
                        .getIdToken()
                        .getTokenString();
                    Log.d(TAG, "idToken = " + idToken);
                    return idToken;
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return null;
                }
            })
            .useClientDatabasePrefix(true)
            .defaultResponseFetcher(responseFetcher)
            .build();
    }

    @NonNull
    static AWSAppSyncClient withUserPools2FromAWSConfiguration(
            String idTokenStringForCustomCognitoUserPool, ResponseFetcher responseFetcher) {
        // Amazon Cognito User Pools - Custom CognitoUserPool
        String clientDatabasePrefix = null;
        String clientName = null;

        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS_2");

        try {
            clientDatabasePrefix = awsConfiguration.optJsonObject("AppSync").getString("ClientDatabasePrefix");
            clientName = awsConfiguration.optJsonObject("AppSync").getString("AuthMode");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AWSAppSyncClient.Builder awsAppSyncClientBuilder4 = AWSAppSyncClient.builder()
                .context(getTargetContext())
                .cognitoUserPoolsAuthProvider(() -> idTokenStringForCustomCognitoUserPool)
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .defaultResponseFetcher(responseFetcher);
        AWSAppSyncClient awsAppSyncClient4 = awsAppSyncClientBuilder4.build();
        assertAWSAppSynClientObjectConstruction(awsAppSyncClient4, clientDatabasePrefix, clientName);
        return awsAppSyncClient4;
    }
}
