/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.apollographql.apollo.api.S3ObjectManager;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DATABASE_NAME_DELIMITER;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_MUTATION_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_QUERY_SQL_STORE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

final class AppSyncTestSetupHelper {
    private static final String TAG = AppSyncTestSetupHelper.class.getSimpleName();
    private static final long EXTENDED_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(30);

    private String bucketName = null;
    private String s3Region = null;
    private String idTokenStringForCustomCognitoUserPool;
    private Context context;

    AppSyncTestSetupHelper() {
        try {
            setupBeforeClass();
        } catch (Exception setupFailure) {
            throw new RuntimeException(setupFailure);
        }
    }

    String getBucketName() {
        return bucketName;
    }

    String getS3Region() {
        return s3Region;
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithApiKeyForGogiTest() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("SubscriptionIntegrationTestGogi");

        return AWSAppSyncClient.builder()
                .context(context)
                .awsConfiguration(awsConfiguration)
                .build();
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithIAMFromAWSConfiguration() {
        return createAppSyncClientWithIAMFromAWSConfiguration(true, 0);
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithIAMFromAWSConfiguration(boolean subscriptionsAutoReconnect, long credentialsDelay) {
        try {
            AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
            awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AWS_IAM");

            final JSONObject ccpConfig = awsConfiguration
                    .optJsonObject("CredentialsProvider")
                    .optJSONObject("CognitoIdentity")
                    .getJSONObject("Default");
            String cognitoIdentityPoolID = ccpConfig.getString("PoolId");
            String cognitoRegion = ccpConfig.getString("Region");

            s3Region = awsConfiguration
                    .optJsonObject("S3TransferUtility")
                    .optJSONObject("Default")
                    .getString("Region");

            bucketName = awsConfiguration
                    .optJsonObject("S3TransferUtility")
                    .optJSONObject("Default")
                    .getString("Bucket");

            AppSyncTestCredentialsProvider credentialsProvider = new AppSyncTestCredentialsProvider(
                    cognitoIdentityPoolID,
                    Regions.fromName(cognitoRegion),
                    credentialsDelay);

            AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider, Region.getRegion(s3Region));
            S3ObjectManager s3ObjectManager = new S3ObjectManagerImplementation((s3Client));

            return AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getContext())
                    .credentialsProvider(credentialsProvider)
                    .awsConfiguration(awsConfiguration)
                    .conflictResolver(new TestConflictResolver())
                    .s3ObjectManager(s3ObjectManager)
                    .subscriptionsAutoReconnect(subscriptionsAutoReconnect)
                    .mutationQueueExecutionTimeout(30 * 1000)
                    .persistentMutationsCallback(new PersistentMutationsCallback() {
                        @Override
                        public void onResponse(PersistentMutationsResponse response) {
                            Log.d(TAG, response.getMutationClassName());
                        }

                        @Override
                        public void onFailure(PersistentMutationsError error) {
                            Log.e(TAG, error.getMutationClassName());
                            Log.e(TAG, "Error", error.getException());
                        }
                    })
                    .useClientDatabasePrefix(true)
                    .build();
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithAPIKEYFromAWSConfiguration() {
        return createAppSyncClientWithAPIKEYFromAWSConfiguration(true, 0);
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithAPIKEYFromAWSConfiguration(boolean subscriptionsAutoReconnect, long credentialsDelay) {
        try {
            AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());

            final JSONObject ccpConfig = awsConfiguration
                    .optJsonObject("CredentialsProvider")
                    .optJSONObject("CognitoIdentity")
                    .getJSONObject("Default");
            String cognitoIdentityPoolID = ccpConfig.getString("PoolId");
            String cognitoRegion = ccpConfig.getString("Region");

            s3Region = awsConfiguration
                    .optJsonObject("S3TransferUtility")
                    .getString("Region");

            bucketName = awsConfiguration
                    .optJsonObject("S3TransferUtility")
                    .getString("Bucket");

            AppSyncTestCredentialsProvider credentialsProvider = new AppSyncTestCredentialsProvider(
                    cognitoIdentityPoolID,
                    Regions.fromName(cognitoRegion),
                    credentialsDelay);

            AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider, Region.getRegion(s3Region));
            S3ObjectManager s3ObjectManager = new S3ObjectManagerImplementation((s3Client));

            return AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getContext())
                    .awsConfiguration(awsConfiguration)
                    .conflictResolver(new TestConflictResolver())
                    .s3ObjectManager(s3ObjectManager)
                    .subscriptionsAutoReconnect(subscriptionsAutoReconnect)
                    .mutationQueueExecutionTimeout(30 * 1000)
                    .persistentMutationsCallback(new PersistentMutationsCallback() {
                        @Override
                        public void onResponse(PersistentMutationsResponse response) {
                            Log.d(TAG, response.getMutationClassName());
                        }

                        @Override
                        public void onFailure(PersistentMutationsError error) {
                            Log.e(TAG, error.getMutationClassName());
                            Log.e(TAG, "Error", error.getException());
                        }
                    })
                    .useClientDatabasePrefix(true)
                    .build();
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithUserPoolsFromAWSConfiguration() {
        // Amazon Cognito User Pools
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS");

        return AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .cognitoUserPoolsAuthProvider(() -> {
                    try {
                        String idToken = TestAWSMobileClient.instance(context)
                            .getTokens()
                            .getIdToken()
                            .getTokenString();
                        Log.d(TAG, "idToken = " + idToken);
                        return idToken;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .useClientDatabasePrefix(true)
                .build();
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithUserPools2FromAWSConfiguration() {
        // Amazon Cognito User Pools - Custom CognitoUserPool
        String clientDatabasePrefix = null;
        String clientName = null;

        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS_2");

        try {
            clientDatabasePrefix = awsConfiguration.optJsonObject("AppSync").getString("ClientDatabasePrefix");
            clientName = awsConfiguration.optJsonObject("AppSync").getString("AuthMode");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AWSAppSyncClient.Builder awsAppSyncClientBuilder4 = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .cognitoUserPoolsAuthProvider(() -> idTokenStringForCustomCognitoUserPool)
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true);
        AWSAppSyncClient awsAppSyncClient4 = awsAppSyncClientBuilder4.build();
        assertAWSAppSynClientObjectConstruction(awsAppSyncClient4, clientDatabasePrefix, clientName);
        return awsAppSyncClient4;
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithUserPoolsFromAWSConfiguration(ResponseFetcher responseFetcher) {
        // Amazon Cognito User Pools
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS");

        return AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .cognitoUserPoolsAuthProvider(() -> {
                    try {
                        String idToken = TestAWSMobileClient.instance(context)
                            .getTokens()
                            .getIdToken()
                            .getTokenString();
                        Log.d(TAG, "idToken = " + idToken);
                        return idToken;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .useClientDatabasePrefix(true)
                .defaultResponseFetcher(responseFetcher)
                .build();
    }

    @NonNull
    AWSAppSyncClient createAppSyncClientWithUserPools2FromAWSConfiguration(ResponseFetcher responseFetcher) {
        // Amazon Cognito User Pools - Custom CognitoUserPool
        String clientDatabasePrefix = null;
        String clientName = null;

        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS_2");

        try {
            clientDatabasePrefix = awsConfiguration.optJsonObject("AppSync").getString("ClientDatabasePrefix");
            clientName = awsConfiguration.optJsonObject("AppSync").getString("AuthMode");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AWSAppSyncClient.Builder awsAppSyncClientBuilder4 = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .cognitoUserPoolsAuthProvider(() -> idTokenStringForCustomCognitoUserPool)
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .defaultResponseFetcher(responseFetcher);
        AWSAppSyncClient awsAppSyncClient4 = awsAppSyncClientBuilder4.build();
        assertAWSAppSynClientObjectConstruction(awsAppSyncClient4, clientDatabasePrefix, clientName);
        return awsAppSyncClient4;
    }

    static class AppSyncTestCredentialsProvider implements AWSCredentialsProvider {
        AWSCredentialsProvider credentialsProvider;
        long credentialsDelay;

        AppSyncTestCredentialsProvider(String cognitoIdentityPoolID, Regions region, long credentialsDelay) {
            this.credentialsProvider = new CognitoCachingCredentialsProvider(InstrumentationRegistry.getContext(), cognitoIdentityPoolID, region);
            this.credentialsDelay = credentialsDelay;
        }

        @Override
        public AWSCredentials getCredentials() {
            if (credentialsDelay > 0) {
                Sleep.milliseconds(credentialsDelay);
            }
            return credentialsProvider.getCredentials();
        }

        @Override
        public void refresh() {
            credentialsProvider.refresh();
        }
    }

    static class TestConflictResolver implements ConflictResolverInterface {
        private static final String TAG = TestConflictResolver.class.getSimpleName();

        @Override
        public void resolveConflict(ConflictResolutionHandler handler,
                                    JSONObject serverState,
                                    JSONObject clientState,
                                    String recordIdentifier,
                                    String operationType) {

            Log.v(TAG, "OperationType is [" + operationType + "]");
            if (operationType.equalsIgnoreCase("UpdateArticleMutation")) {
                try {
                    if (clientState == null ) {
                        Log.v(TAG, "Failing conflict as client state was null");
                        handler.fail(recordIdentifier);
                        return;
                    }

                    //For the purposes of this test conflict handler
                    //we will fail mutations if the title is ALWAYS DISCARD.
                    JSONObject input = clientState.getJSONObject("input");
                    if (input == null ) {
                        Log.v(TAG, "Failing conflict as input was null");
                        handler.fail(recordIdentifier);
                        return;
                    }

                    final String title = input.getString("title");

                    if ("ALWAYS DISCARD".equals(title)) {
                        Log.v(TAG, "Failing conflict as title was ALWAYS DISCARD");
                        handler.fail(recordIdentifier);
                        return;
                    }

                    String id = input.getString("id");
                    String author = input.getString("author");

                    if (id == null || author == null || title == null ) {
                        Log.v(TAG, "Failing conflict as id, author or title was null");
                        handler.fail(recordIdentifier);
                    }
                    int resolvedVersion = serverState.getInt("version");
                    if ("RESOLVE_CONFLICT_INCORRECTLY".equals(title)) {
                        //This will fail again.
                        Log.v(TAG, "Resolving conflict incorrectly");
                        resolvedVersion = resolvedVersion - 1;
                    }
                    else {
                        Log.v(TAG, "Resolving conflict correctly");
                    }

                    UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                            .id(id)
                            .title(title)
                            .author(author)
                            .expectedVersion(resolvedVersion)
                            .build();

                    UpdateArticleMutation updateArticleMutation = UpdateArticleMutation.builder().input(updateArticleInput).build();

                    handler.retryMutation(updateArticleMutation, recordIdentifier);
                } catch (JSONException je) {
                    je.printStackTrace();
                    // in case of un-expected errors, we fail the mutation
                    // we can also call the below method if we want server data to be accepted instead of client.
                    handler.fail(recordIdentifier);
                }
            }
            else {
                handler.fail(recordIdentifier);
            }
        }
    }

    private void setupBeforeClass() throws Exception {
        context = InstrumentationRegistry.getTargetContext();

        // Pass the results of the callback into the provided consumers.
        // The consumers in turn are wired to the Await utility, which will block until one of the
        // consumers accepts a value.
        Await.result((Await.ResultErrorEmitter<SignInResult, Exception>) (onResult, onError) -> {
            DelegatingCallback<SignInResult> callback = DelegatingCallback.to(onResult, onError);
            TestAWSMobileClient.instance(context)
                .signIn("appsync-multi-auth-test-user", "P@ssw0rd!", null, callback);
        });
        AWSConfiguration awsConfigurationForCognitoUserPool = new AWSConfiguration(context);
        awsConfigurationForCognitoUserPool.setConfiguration("Custom");
        CognitoUserPool cognitoUserPool = new CognitoUserPool(context, awsConfigurationForCognitoUserPool);

        final CountDownLatch countDownLatchForSignIn = new CountDownLatch(1);
        cognitoUserPool.getUser("appsync-multi-auth-test-user").getSession(new AuthenticationHandler() {
            @Override
            public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                idTokenStringForCustomCognitoUserPool = userSession.getIdToken().getJWTToken();
                countDownLatchForSignIn.countDown();
            }

            @Override
            public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                Log.d(TAG, "Sending password.");
                authenticationContinuation.setAuthenticationDetails(new AuthenticationDetails(
                    "appsync-multi-auth-test-user", "P@ssw0rd!", null
                ));
                authenticationContinuation.continueTask();
            }

            @Override
            public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
            }

            @Override
            public void authenticationChallenge(ChallengeContinuation continuation) {
            }

            @Override
            public void onFailure(Exception exception) {
            }
        });
        Await.latch(countDownLatchForSignIn, TimeUnit.SECONDS.toMillis(EXTENDED_WAIT_TIME_MS));
        assertNotNull("ID Token String for Cognito User Pool cannot be null.", idTokenStringForCustomCognitoUserPool);
    }

    void assertAWSAppSynClientObjectConstruction(AWSAppSyncClient awsAppSyncClient,
                                                 String clientDatabasePrefix,
                                                 String clientName) {
        assertNotNull(awsAppSyncClient);
        assertNotNull(awsAppSyncClient.mSyncStore);
        if (clientDatabasePrefix != null) {
            assertEquals(clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_QUERY_SQL_STORE_NAME, awsAppSyncClient.querySqlStoreName);
            assertEquals(clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_MUTATION_SQL_STORE_NAME, awsAppSyncClient.mutationSqlStoreName);
            assertEquals(clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_DELTA_SYNC_SQL_STORE_NAME, awsAppSyncClient.deltaSyncSqlStoreName);
        } else {
            assertEquals(DEFAULT_QUERY_SQL_STORE_NAME, awsAppSyncClient.querySqlStoreName);
            assertEquals(DEFAULT_MUTATION_SQL_STORE_NAME, awsAppSyncClient.mutationSqlStoreName);
            assertEquals(DEFAULT_DELTA_SYNC_SQL_STORE_NAME, awsAppSyncClient.deltaSyncSqlStoreName);
        }
    }
}
