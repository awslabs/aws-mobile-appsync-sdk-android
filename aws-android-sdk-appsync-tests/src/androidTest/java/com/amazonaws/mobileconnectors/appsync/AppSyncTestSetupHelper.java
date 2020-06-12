/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMissingRequiredFieldsMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostRequiredFieldsOnlyMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllArticlesQuery;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.DeletePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.DeletePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
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
import com.amazonaws.apollographql.apollo.GraphQLCall;
import com.amazonaws.apollographql.apollo.api.Response;
import com.amazonaws.apollographql.apollo.api.S3ObjectManager;
import com.amazonaws.apollographql.apollo.exception.ApolloException;
import com.amazonaws.apollographql.apollo.fetcher.ResponseFetcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DATABASE_NAME_DELIMITER;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_MUTATION_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_QUERY_SQL_STORE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppSyncTestSetupHelper {

    public AppSyncTestSetupHelper() {
        setupBeforeClass();
    }

    private final static String TAG = AppSyncTestSetupHelper.class.getSimpleName();
    private String bucketName = null;
    private String s3Region = null;

    public String getBucketName() {
        return bucketName;
    }

    public String getS3Region() {
        return s3Region;
    }

    public AWSAppSyncClient createAppSyncClientWithApiKeyForGogiTest() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("SubscriptionIntegrationTestGogi");

        return AWSAppSyncClient.builder()
                .context(context)
                .awsConfiguration(awsConfiguration)
                .build();
    }

    public AWSAppSyncClient createAppSyncClientWithIAMFromAWSConfiguration() {
        return createAppSyncClientWithIAMFromAWSConfiguration(true, 0);
    }

    public AWSAppSyncClient createAppSyncClientWithIAMFromAWSConfiguration(boolean subscriptionsAutoReconnect, long credentialsDelay) {
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
        } catch (JSONException je) {
            Log.e(TAG, je.getLocalizedMessage());
            je.printStackTrace();
        }

        return null;
    }

    public AWSAppSyncClient createAppSyncClientWithAPIKEYFromAWSConfiguration() {
        return createAppSyncClientWithAPIKEYFromAWSConfiguration(true, 0);
    }

    public AWSAppSyncClient createAppSyncClientWithAPIKEYFromAWSConfiguration(boolean subscriptionsAutoReconnect, long credentialsDelay) {
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
        } catch (JSONException je) {
            Log.e(TAG, je.getLocalizedMessage());
            je.printStackTrace();
        }

        return null;
    }

    public AWSAppSyncClient createAppSyncClientWithUserPoolsFromAWSConfiguration() {
        // Amazon Cognito User Pools
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS");

        return AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .cognitoUserPoolsAuthProvider(new CognitoUserPoolsAuthProvider() {
                    @Override
                    public String getLatestAuthToken() {
                        try {
                            String idToken = AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString();
                            Log.d(TAG, "idToken = " + idToken);
                            return idToken;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .useClientDatabasePrefix(true)
                .build();
    }

    public AWSAppSyncClient createAppSyncClientWithUserPools2FromAWSConfiguration() {
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
                .cognitoUserPoolsAuthProvider(new CognitoUserPoolsAuthProvider() {
                    @Override
                    public String getLatestAuthToken() {
                        return idTokenStringForCustomCognitoUserPool;
                    }
                })
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true);
        AWSAppSyncClient awsAppSyncClient4 = awsAppSyncClientBuilder4.build();
        assertAWSAppSynClientObjectConstruction(awsAppSyncClient4, clientDatabasePrefix, clientName);
        return awsAppSyncClient4;
    }

    public AWSAppSyncClient createAppSyncClientWithUserPoolsFromAWSConfiguration(ResponseFetcher responseFetcher) {
        // Amazon Cognito User Pools
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_AMAZON_COGNITO_USER_POOLS");

        return AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .cognitoUserPoolsAuthProvider(new CognitoUserPoolsAuthProvider() {
                    @Override
                    public String getLatestAuthToken() {
                        try {
                            String idToken = AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString();
                            Log.d(TAG, "idToken = " + idToken);
                            return idToken;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .useClientDatabasePrefix(true)
                .defaultResponseFetcher(responseFetcher)
                .build();
    }

    public AWSAppSyncClient createAppSyncClientWithUserPools2FromAWSConfiguration(ResponseFetcher responseFetcher) {
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
                .cognitoUserPoolsAuthProvider(new CognitoUserPoolsAuthProvider() {
                    @Override
                    public String getLatestAuthToken() {
                        return idTokenStringForCustomCognitoUserPool;
                    }
                })
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .defaultResponseFetcher(responseFetcher);
        AWSAppSyncClient awsAppSyncClient4 = awsAppSyncClientBuilder4.build();
        assertAWSAppSynClientObjectConstruction(awsAppSyncClient4, clientDatabasePrefix, clientName);
        return awsAppSyncClient4;
    }

    public String createDataFile(String fileName, String data) {
        File f = new File(InstrumentationRegistry.getContext().getCacheDir() + fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(data.getBytes());
        }
        catch (IOException e) {
            return null;
        }
        finally {
            try {
                fos.close();
            }
            catch (IOException e) {

            }
        }
        return f.getAbsolutePath();
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
                try {
                    //Inject a delay so that we can mimic the behavior of mutations/subscription requests being
                    //cancelled during execution. See "testCrud" for an example.
                    Thread.sleep(credentialsDelay);
                } catch (Exception e) {
                    Log.v(TAG, "Thread sleep was interrupted [" + e +"]");
                }
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

    String idTokenStringForCustomCognitoUserPool;
    AWSConfiguration awsConfiguration;

    Context context;
    Response<AddPostMutation.Data> addPostMutationResponse;

    public void setupBeforeClass() {
        context = InstrumentationRegistry.getTargetContext();
        awsConfiguration = new AWSConfiguration(context);
        final CountDownLatch countDownLatchForInitialize = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(InstrumentationRegistry.getTargetContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                countDownLatchForInitialize.countDown();
            }

            @Override
            public void onError(Exception e) {
                countDownLatchForInitialize.countDown();
            }
        });

        try {
            countDownLatchForInitialize.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        AWSMobileClient.getInstance().signIn("appsync-multi-auth-test-user",
                "P@ssw0rd!",
                null,
                new Callback<SignInResult>() {
                    @Override
                    public void onResult(SignInResult result) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        countDownLatch.countDown();
                    }
                });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AWSConfiguration awsConfigurationForCognitoUserPool = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfigurationForCognitoUserPool.setConfiguration("Custom");
        CognitoUserPool cognitoUserPool = new CognitoUserPool(InstrumentationRegistry.getTargetContext(), awsConfigurationForCognitoUserPool);
        final CountDownLatch countDownLatchForSignIn = new CountDownLatch(1);

        try {
            cognitoUserPool.getUser("appsync-multi-auth-test-user").getSession(new AuthenticationHandler() {

                @Override
                public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                    idTokenStringForCustomCognitoUserPool = userSession.getIdToken().getJWTToken();
                    countDownLatchForSignIn.countDown();
                }

                @Override
                public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                    Log.d(TAG, "Sending password.");
                    authenticationContinuation.setAuthenticationDetails(
                            new AuthenticationDetails(
                                    "appsync-multi-auth-test-user",
                                    "P@ssw0rd!",
                                    null));
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

            countDownLatchForSignIn.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertNotNull("ID Token String for Cognito User Pool cannot be null.", idTokenStringForCustomCognitoUserPool);
    }

    void assertAWSAppSynClientObjectConstruction(AWSAppSyncClient awsAppSyncClient,
                                                 String clientDatabasePrefix,
                                                 @Nonnull String clientName) {
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

    void testCRUD(List<AWSAppSyncClient> awsAppSyncClients) {
        for (final AWSAppSyncClient awsAppSyncClient : awsAppSyncClients) {
            assertNotNull(awsAppSyncClient);
            final String title = "Home [Scene Six]";
            final String author = "Dream Theater @ " + System.currentTimeMillis();
            final String url = "Metropolis Part 2";
            final String content = "Shine-Lake of fire @" + System.currentTimeMillis();

            addPost(awsAppSyncClient, title, author, url, content);
            addPostAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            addPost(awsAppSyncClient, title, author, url, content);
            addPostAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            addPost(awsAppSyncClient, title, author, url, content);
            addPostAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            addPost(awsAppSyncClient, title, author, url, content);
            addPostAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            addPost(awsAppSyncClient, title, author, url, content);
            addPostAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            addPost(awsAppSyncClient, title, author, url, content);
            addPostAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);

            //Add a post
            Response<AddPostMutation.Data> addPostMutationResponse = addPost(awsAppSyncClient, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            assertNotNull(addPostMutationResponse.data().createPost());
            assertNotNull(addPostMutationResponse.data().createPost().id());

            String postID = addPostMutationResponse.data().createPost().id();
            Log.d(TAG, "Added Post ID: " + postID);

            //Check that the post has been propagated to the server
            Response<GetPostQuery.Data> getPostQueryResponse = queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertEquals(postID, getPostQueryResponse.data().getPost().id());
            assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));

            //Check that the post has been propagated to the server
            getPostQueryResponse = queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertEquals(postID, getPostQueryResponse.data().getPost().id());
            assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));


            //Check that the post has been made it to the cache
            getPostQueryResponse = queryPost(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY, postID).get("CACHE");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertTrue(postID.equals(getPostQueryResponse.data().getPost().id()));
            assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));


            //Update the Post
            final String updatedContent = "New content coming up @" + System.currentTimeMillis();

            Response<UpdatePostMutation.Data> updatePostMutationResponse = updatePost(awsAppSyncClient, postID, updatedContent);
            assertNotNull(updatePostMutationResponse);
            assertNotNull(updatePostMutationResponse.data());
            assertNotNull(updatePostMutationResponse.data().updatePost());
            assertNotNull(updatePostMutationResponse.data().updatePost().content());
            assertEquals(true, updatedContent.equals(updatePostMutationResponse.data().updatePost().content()));
            assertEquals(false, content.equals(updatePostMutationResponse.data().updatePost().content()));

            //Check that the information has been propagated to the server
            getPostQueryResponse = queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_FIRST, postID).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().id());
            assertEquals(true, postID.equals(getPostQueryResponse.data().getPost().id()));
            assertEquals(false, content.equals(getPostQueryResponse.data().getPost().content()));
            assertEquals(true, updatedContent.equals(getPostQueryResponse.data().getPost().content()));

            //Check that the information has been updated in the local cache
            getPostQueryResponse = queryPost(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY, postID).get("CACHE");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().id());
            assertEquals(true, postID.equals(getPostQueryResponse.data().getPost().id()));
            assertEquals(false, content.equals(getPostQueryResponse.data().getPost().content()));
            assertEquals(true, updatedContent.equals(getPostQueryResponse.data().getPost().content()));


            //Delete the Post
            Response<DeletePostMutation.Data> deletePostMutationResponse = deletePost(awsAppSyncClient, postID);
            assertNotNull(deletePostMutationResponse);
            assertNotNull(deletePostMutationResponse.data());
            assertNotNull(deletePostMutationResponse.data().deletePost());
            assertNotNull(deletePostMutationResponse.data().deletePost().id());
            assertTrue(postID.equals(deletePostMutationResponse.data().deletePost().id()));

            //Check that it is gone from the server
            getPostQueryResponse = queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNull(getPostQueryResponse.data().getPost());
        }
    }

    Map<String, Response<GetPostQuery.Data>> queryPost(AWSAppSyncClient awsAppSyncClient,
                                                       final ResponseFetcher responseFetcher,
                                                       final String id) {

        Log.d(TAG, "Calling Query queryPost with responseFetcher: " + responseFetcher.toString());

        final CountDownLatch queryCountDownLatch;
        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            queryCountDownLatch = new CountDownLatch(2);
        } else {
            queryCountDownLatch = new CountDownLatch(1);
        }

        final Map<String, Response<GetPostQuery.Data>> getPostQueryResponses = new HashMap<>();
        awsAppSyncClient.query(GetPostQuery.builder().id(id).build())
                .responseFetcher(responseFetcher)
                .enqueue(new GraphQLCall.Callback<GetPostQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<GetPostQuery.Data> response) {
                        if (response.fromCache()) {
                            getPostQueryResponses.put("CACHE", response);
                        } else {
                            getPostQueryResponses.put("NETWORK", response);
                        }
                        queryCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        e.printStackTrace();
                        queryCountDownLatch.countDown();
                    }
                });
        try {
            assertTrue(queryCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        Log.d(TAG, "responseFetcher: " + responseFetcher.toString() + "; Cache response: " + getPostQueryResponses.get("CACHE"));
        Log.d(TAG, "responseFetcher: " + responseFetcher.toString() + "; Network response: " + getPostQueryResponses.get("NETWORK"));

        return getPostQueryResponses;
    }

    Map<String, Response<AllPostsQuery.Data>> listPosts(AWSAppSyncClient awsAppSyncClient,
                                                        final ResponseFetcher responseFetcher) {
        final CountDownLatch queryCountDownLatch;
        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            queryCountDownLatch = new CountDownLatch(2);
        } else {
            queryCountDownLatch = new CountDownLatch(1);
        }

        Log.d(TAG, "Calling Query listPosts with responseFetcher: " + responseFetcher.toString());

        final Map<String, Response<AllPostsQuery.Data>> listPostsQueryResponses = new HashMap<>();
        awsAppSyncClient.query(AllPostsQuery.builder().build())
                .responseFetcher(responseFetcher)
                .enqueue(new GraphQLCall.Callback<AllPostsQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                        if (response.fromCache()) {
                            listPostsQueryResponses.put("CACHE", response);
                        } else {
                            listPostsQueryResponses.put("NETWORK", response);
                        }
                        queryCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        e.printStackTrace();
                        queryCountDownLatch.countDown();
                    }
                });
        try {
            assertTrue(queryCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            assertNotNull(listPostsQueryResponses.get("CACHE"));
            assertNotNull(listPostsQueryResponses.get("NETWORK"));
            assertEquals(2, listPostsQueryResponses.size());
        } else {
            assertEquals(1, listPostsQueryResponses.size());
        }

        return listPostsQueryResponses;
    }

    Map<String, Response<AllArticlesQuery.Data>> listArticles(AWSAppSyncClient awsAppSyncClient,
                                                              final ResponseFetcher responseFetcher) {
        final CountDownLatch queryCountDownLatch;
        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            queryCountDownLatch = new CountDownLatch(2);
        } else {
            queryCountDownLatch = new CountDownLatch(1);
        }

        Log.d(TAG, "Calling Query listArticles");
        final Map<String, Response<AllArticlesQuery.Data>> responses = new HashMap<>();
        awsAppSyncClient.query(AllArticlesQuery.builder().build())
                .responseFetcher(responseFetcher)
                .enqueue(new GraphQLCall.Callback<AllArticlesQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<AllArticlesQuery.Data> response) {
                        if (response.fromCache()) {
                            responses.put("CACHE", response);
                        } else {
                            responses.put("NETWORK", response);
                        }
                        queryCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        e.printStackTrace();
                        queryCountDownLatch.countDown();
                    }
                });
        try {
            assertTrue(queryCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            assertNotNull(responses.get("CACHE"));
            assertNotNull(responses.get("NETWORK"));
            assertEquals(2, responses.size());
        } else {
            assertEquals(1, responses.size());
        }

        return responses;
    }

    Response<AddPostMutation.Data> addPost(final AWSAppSyncClient awsAppSyncClient, final String title, final String author, final String url, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        final List<Response<AddPostMutation.Data>> responses = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                AddPostMutation.Data expected = new AddPostMutation.Data(new AddPostMutation.CreatePost(
                        "Post",
                        "",
                        "",
                        "",
                        "",
                        "",
                        null,
                        null,
                        0
                ));

                CreatePostInput createPostInput = CreatePostInput.builder()
                        .title(title)
                        .author(author)
                        .url(url)
                        .content(content)
                        .ups(new Integer(1))
                        .downs(new Integer(0))
                        .build();

                AddPostMutation addPostMutation = AddPostMutation.builder().input(createPostInput).build();
                AppSyncMutationCall call = awsAppSyncClient.mutate(addPostMutation, expected);
                call.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                        responses.add(response);
                        if (Looper.myLooper() != null) {
                            Looper.myLooper().quit();
                        }
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull final ApolloException e) {
                        e.printStackTrace();
                        if (Looper.myLooper() != null) {
                            Looper.myLooper().quit();
                        }
                        mCountDownLatch.countDown();
                    }
                });

                Looper.loop();
            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        assertTrue("addPost should have a valid response: ", responses.size() > 0);
        return responses.get(0);
    }

    void addPostAndCancel(final AWSAppSyncClient awsAppSyncClient, final String title, final String author, final String url, final String content) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                AddPostMutation.Data expected = new AddPostMutation.Data(new AddPostMutation.CreatePost(
                        "Post",
                        "",
                        "",
                        "",
                        "",
                        "",
                        null,
                        null,
                        0
                ));

                CreatePostInput createPostInput = CreatePostInput.builder()
                        .title(title)
                        .author(author)
                        .url(url)
                        .content(content)
                        .ups(new Integer(1))
                        .downs(new Integer(0))
                        .build();

                AddPostMutation addPostMutation = AddPostMutation.builder().input(createPostInput).build();

                AppSyncMutationCall call = awsAppSyncClient.mutate(addPostMutation, expected);
                call.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                        addPostMutationResponse = response;
                        if (Looper.myLooper() != null) {
                            Looper.myLooper().quit();
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull final ApolloException e) {
                        Log.v(TAG, "On Failure called for add Post");
                        e.printStackTrace();
                        //Set to null to indicate failure
                        addPostMutationResponse = null;
                        if (Looper.myLooper() != null) {
                            Looper.myLooper().quit();
                        }
                    }
                });

                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                call.cancel();
                Looper.loop();

            }
        }).start();
    }

    Response<AddPostRequiredFieldsOnlyMutation.Data> addPostRequiredFieldsOnlyMutation(final AWSAppSyncClient awsAppSyncClient, final String title, final String author, final String url, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        final List<Response<AddPostRequiredFieldsOnlyMutation.Data>> responses = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                AddPostRequiredFieldsOnlyMutation.Data expected = new AddPostRequiredFieldsOnlyMutation.Data( null);

                CreatePostInput createPostInput = CreatePostInput.builder()
                        .title(title)
                        .author(author)
                        .url(url)
                        .content(content)
                        .ups(1)
                        .downs(0)
                        .build();

                final AddPostRequiredFieldsOnlyMutation addPostRequiredFieldsOnlyMutation = AddPostRequiredFieldsOnlyMutation.builder().input(createPostInput).build();

                awsAppSyncClient
                        .mutate(addPostRequiredFieldsOnlyMutation, expected)
                        .enqueue(new GraphQLCall.Callback<AddPostRequiredFieldsOnlyMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<AddPostRequiredFieldsOnlyMutation.Data> response) {
                                responses.add(response);
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        return responses.get(0);
    }

    Response<AddPostMissingRequiredFieldsMutation.Data> addPostMissingRequiredFieldsMutation(final AWSAppSyncClient awsAppSyncClient, final String title, final String author, final String url, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        final List<Response<AddPostMissingRequiredFieldsMutation.Data>> responses = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                AddPostMissingRequiredFieldsMutation.Data expected = new AddPostMissingRequiredFieldsMutation.Data(null);

                CreatePostInput createPostInput = CreatePostInput.builder()
                        .title(title)
                        .author(author)
                        .url(url)
                        .content(content)
                        .ups(1)
                        .downs(0)
                        .build();

                final AddPostMissingRequiredFieldsMutation missingRequiredFieldsMutation = AddPostMissingRequiredFieldsMutation.builder().input(createPostInput).build();


                awsAppSyncClient
                        .mutate(missingRequiredFieldsMutation, expected)
                        .enqueue(new GraphQLCall.Callback<AddPostMissingRequiredFieldsMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<AddPostMissingRequiredFieldsMutation.Data> response) {
                                responses.add(response);
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        return responses.get(0);
    }

    Response<DeletePostMutation.Data> deletePost(final AWSAppSyncClient awsAppSyncClient, final String id) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        final List<Response<DeletePostMutation.Data>> responses = new ArrayList<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                DeletePostMutation.Data expected = new DeletePostMutation.Data(new DeletePostMutation.DeletePost(
                        "Post",
                        "",
                        "",
                        "",
                        "",
                        ""
                ));

                DeletePostInput deletePostInput = DeletePostInput.builder()
                        .id(id)
                        .build();

                DeletePostMutation deletePostMutation = DeletePostMutation.builder().input(deletePostInput).build();

                Log.d(TAG, "Calling Delete");

                awsAppSyncClient
                        .mutate(deletePostMutation, expected)
                        .enqueue(new GraphQLCall.Callback<DeletePostMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<DeletePostMutation.Data> response) {
                                responses.add(response);
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        return responses.get(0);
    }

    Response<UpdatePostMutation.Data> updatePost(final AWSAppSyncClient awsAppSyncClient, final String postID, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        final List<Response<UpdatePostMutation.Data>> responses = new ArrayList<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                UpdatePostMutation.Data expected = new UpdatePostMutation.Data( new UpdatePostMutation.UpdatePost(
                        "Post",
                        postID,
                        "",
                        "",
                        content,
                        "",
                        0
                ));

                UpdatePostInput updatePostInput = UpdatePostInput.builder()
                        .id(postID)
                        .content(content)
                        .build();

                UpdatePostMutation updatePostMutation = UpdatePostMutation.builder().input(updatePostInput).build();

                Log.d(TAG, "Calling Update");

                awsAppSyncClient
                        .mutate(updatePostMutation, expected)
                        .enqueue(new GraphQLCall.Callback<UpdatePostMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<UpdatePostMutation.Data> response) {
                                responses.add(response);
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                Log.d(TAG, "On error called");
                                e.printStackTrace();
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        assertTrue("There should be a valid response for the mutation", responses.size() > 0);
        return responses.get(0);
    }

    void assertQueryPostResponse(Response<GetPostQuery.Data> response, String postID, String authMode) {
        assertNotNull(response);
        assertNotNull(response.data());
        assertNotNull(response.data().getPost());

        Log.d(TAG, "isFromCache: " + response.fromCache());
        Log.d(TAG, "Post Details: " + response.data().getPost().toString());

        assertNotNull(response.data().getPost().id());
        assertEquals(postID, response.data().getPost().id());
        assertNotNull(response.data().getPost().author());
        assertNotNull(response.data().getPost().content());
        assertNotNull(response.data().getPost().title());
        assertNotNull(response.data().getPost().version());

        if ("AMAZON_COGNITO_USER_POOLS".equals(authMode)) {
            assertNotNull(response.data().getPost().url());
            assertNotNull(response.data().getPost().ups());
            assertNotNull(response.data().getPost().downs());
        } else {
            assertNull(response.data().getPost().url());
            assertNull(response.data().getPost().ups());
            assertNull(response.data().getPost().downs());
        }
    }

    void sleep(int time) {
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException ie) {
            Log.d(TAG, "Sleep was interrupted");
        }

    }
}
