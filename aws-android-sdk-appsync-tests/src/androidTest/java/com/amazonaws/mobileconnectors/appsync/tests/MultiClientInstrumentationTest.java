/*
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.tests;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClientException;
import com.amazonaws.mobileconnectors.appsync.ClearCacheException;
import com.amazonaws.mobileconnectors.appsync.ClearCacheOptions;
import com.amazonaws.mobileconnectors.appsync.SyncStore;
import com.amazonaws.mobileconnectors.appsync.client.AWSAppSyncClients;
import com.amazonaws.mobileconnectors.appsync.client.LatchedGraphQLCallback;
import com.amazonaws.mobileconnectors.appsync.client.NoOpGraphQLCallback;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.identity.CustomCognitoUserPool;
import com.amazonaws.mobileconnectors.appsync.models.PostCruds;
import com.amazonaws.mobileconnectors.appsync.models.Posts;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.util.Await;
import com.amazonaws.mobileconnectors.appsync.util.JsonExtract;
import com.amazonaws.mobileconnectors.appsync.util.Wifi;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Operation.Variables;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MultiClientInstrumentationTest {
    private static final String TAG = MultiClientInstrumentationTest.class.getSimpleName();
    private static String idToken = null;

    @BeforeClass
    public static void setupOnce() {
        idToken = CustomCognitoUserPool.setup();
    }

    @Before
    @After
    public void ensureNetworkIsUp() {
        Wifi.turnOn();
    }

    @Test
    public void testSyncOnlyBaseQuery() {
        List<AWSAppSyncClient> clients = Arrays.asList(
            AWSAppSyncClients.withAPIKEYFromAWSConfiguration(),
            AWSAppSyncClients.withIAMFromAWSConfiguration(),
            AWSAppSyncClients.withUserPoolsFromAWSConfiguration()
        );

        for (AWSAppSyncClient client : clients) {
            Query<AllPostsQuery.Data, AllPostsQuery.Data, Variables> baseQuery = AllPostsQuery.builder().build();
            LatchedGraphQLCallback<Query.Data> baseQueryCallback = LatchedGraphQLCallback.instance();
            Cancelable handle = client.sync(baseQuery, baseQueryCallback, 0);
            assertFalse(handle.isCanceled());
            baseQueryCallback.awaitSuccessfulResponse();

            handle.cancel();
            assertTrue(handle.isCanceled());

            // This should be a No-op. Test to make sure.
            handle.cancel();
            assertTrue(handle.isCanceled());
        }
    }

    @Test
    public void testSyncOnlyBaseAndDeltaQuery() {
        List<AWSAppSyncClient> clients = Arrays.asList(
            AWSAppSyncClients.withAPIKEYFromAWSConfiguration(),
            AWSAppSyncClients.withIAMFromAWSConfiguration(),
            AWSAppSyncClients.withUserPoolsFromAWSConfiguration()
        );

        for (AWSAppSyncClient client : clients) {
            AllPostsQuery baseQuery = AllPostsQuery.builder().build();
            LatchedGraphQLCallback<Query.Data> baseQueryCallback = LatchedGraphQLCallback.instance();
            AllPostsQuery deltaQuery = AllPostsQuery.builder().build();
            LatchedGraphQLCallback<Query.Data> deltaQueryCallback = LatchedGraphQLCallback.instance();

            Cancelable handle =client.sync(baseQuery, baseQueryCallback, deltaQuery, deltaQueryCallback, 5);
            assertFalse(handle.isCanceled());
            baseQueryCallback.awaitSuccessfulResponse();
            deltaQueryCallback.awaitSuccessfulResponse();

            handle.cancel();
            assertTrue(handle.isCanceled());

            // This should be a no-op. Test to make sure that there are no unintended side effects.
            handle.cancel();
            assertTrue(handle.isCanceled());
        }
    }

    /**
     * Call sync and get a Base query callback
     * Call sync and get a Delta query callback
     * Call clearCaches to clear the delta sync store
     * Call sync and get a Base query callback
     */
    @Test
    public void testClearDeltaSyncStore() throws ClearCacheException {
        // Arrange client and sync queries
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();
        AllPostsQuery baseQuery = AllPostsQuery.builder().build();
        AllPostsQuery deltaQuery = AllPostsQuery.builder().build();

        // First, perform a base sync.
        LatchedGraphQLCallback<Query.Data> initialBaseSyncCallback = LatchedGraphQLCallback.instance();
        Cancelable firstSyncHandle =
            awsAppSyncClient.sync(baseQuery, initialBaseSyncCallback, deltaQuery, NoOpGraphQLCallback.instance(), 5);
        assertFalse(firstSyncHandle.isCanceled());
        initialBaseSyncCallback.awaitSuccessfulResponse();

        // Now, perform a delta sync.
        LatchedGraphQLCallback<Query.Data> initialDeltaSyncCallback = LatchedGraphQLCallback.instance();
        Cancelable secondSyncHandle =
            awsAppSyncClient.sync(baseQuery, NoOpGraphQLCallback.instance(), deltaQuery, initialDeltaSyncCallback, 5);
        assertFalse(secondSyncHandle.isCanceled());
        initialDeltaSyncCallback.awaitSuccessfulResponse();
        secondSyncHandle.cancel();
        assertTrue(secondSyncHandle.isCanceled());
        firstSyncHandle.cancel();
        assertTrue(firstSyncHandle.isCanceled());

        // Act: Clear the Delta sync store.
        awsAppSyncClient.clearCaches();

        // Now, perform another sync.
        // Assert that the base sync is called this time (TODO: is this right?)
        LatchedGraphQLCallback<Query.Data> syncAfterClearCallback = LatchedGraphQLCallback.instance();
        Cancelable afterClearCallback =
            awsAppSyncClient.sync(baseQuery, syncAfterClearCallback, deltaQuery, NoOpGraphQLCallback.instance(), 5);
        assertFalse(afterClearCallback.isCanceled());
        syncAfterClearCallback.awaitSuccessfulResponse();
        afterClearCallback.cancel();
        assertTrue(afterClearCallback.isCanceled());
    }

    @Test
    public void testCache() {
        List<AWSAppSyncClient> clients = Arrays.asList(
            AWSAppSyncClients.withAPIKEYFromAWSConfiguration(),
            AWSAppSyncClients.withIAMFromAWSConfiguration(),
            AWSAppSyncClients.withUserPoolsFromAWSConfiguration()
        );

        for (AWSAppSyncClient client : clients) {
            Response<AllPostsQuery.Data> allPostsResponse =
                Posts.list(client, AppSyncResponseFetchers.NETWORK_ONLY).get("NETWORK");
            assertNotNull(allPostsResponse);
            assertNotNull(allPostsResponse.data());
            assertNotNull(allPostsResponse.data().listPosts());
            AllPostsQuery.ListPosts listPosts = allPostsResponse.data().listPosts();
            assertNotNull(listPosts);
            List<AllPostsQuery.Item> items = listPosts.items();
            assertNotNull(items);

            String postID;
            long numPostsFromNetwork = 0;
            for (AllPostsQuery.Item item : items) {
                postID = item.id();
                Response<GetPostQuery.Data> getPostQueryResponse =
                    Posts.query(client, AppSyncResponseFetchers.NETWORK_ONLY, postID).get("NETWORK");
                assertNotNull(getPostQueryResponse);
                assertNotNull(getPostQueryResponse.data());
                assertNotNull(getPostQueryResponse.data().getPost());
                assertEquals(postID, getPostQueryResponse.data().getPost().id());
                numPostsFromNetwork++;
            }

            long numPostsFromCache = 0;
            for (AllPostsQuery.Item item : items) {
                postID = item.id();
                Response<GetPostQuery.Data> getPostQueryResponse =
                    Posts.query(client, AppSyncResponseFetchers.CACHE_ONLY, postID).get("CACHE");
                assertNotNull(getPostQueryResponse);
                assertNotNull(getPostQueryResponse.data());
                assertNotNull(getPostQueryResponse.data().getPost());
                assertEquals(postID, getPostQueryResponse.data().getPost().id());
                numPostsFromCache++;
            }

            assertEquals(numPostsFromNetwork, numPostsFromCache);
        }

        clients.clear();
    }

    @Test
    public void testQueryPostsWithAllResponseFetchersAndMultipleClients() {
        final ResponseFetcher[] appSyncResponseFetchers = new ResponseFetcher[] {
            AppSyncResponseFetchers.NETWORK_FIRST,
            AppSyncResponseFetchers.NETWORK_ONLY,
            AppSyncResponseFetchers.CACHE_FIRST
        };

        for (ResponseFetcher appSyncResponseFetcher: appSyncResponseFetchers) {
            Log.d(TAG, "Response Fetcher: " + appSyncResponseFetcher.toString());
            Map<String, AWSAppSyncClient> appSyncClientMap = new HashMap<>();
            appSyncClientMap.put("AMAZON_COGNITO_USER_POOLS", AWSAppSyncClients.withUserPoolsFromAWSConfiguration(appSyncResponseFetcher));
            appSyncClientMap.put("AMAZON_COGNITO_USER_POOLS_2", AWSAppSyncClients.withUserPools2FromAWSConfiguration(idToken, appSyncResponseFetcher));

            for (final String clientName : appSyncClientMap.keySet()) {
                // List Posts
                final AWSAppSyncClient awsAppSyncClient = appSyncClientMap.get(clientName);
                Log.d(TAG, "AWSAppSyncClient for clientName: " + clientName + "; client: " + awsAppSyncClient);
                assertNotNull(awsAppSyncClient);
                Map<String, Response<AllPostsQuery.Data>> listPostsResponses = Posts.list(awsAppSyncClient, appSyncResponseFetcher);
                Response<AllPostsQuery.Data> allPostsResponse = listPostsResponses.get("CACHE") != null
                        ? listPostsResponses.get("CACHE")
                        : listPostsResponses.get("NETWORK");
                assertNotNull(allPostsResponse);
                assertNotNull(allPostsResponse.data());
                AllPostsQuery.ListPosts listPosts = allPostsResponse.data().listPosts();
                assertNotNull(listPosts);
                List<AllPostsQuery.Item> items = listPosts.items();
                assertNotNull(items);

                // Loop over the list and query each post
                String postID;
                for (AllPostsQuery.Item item : items) {
                    postID = item.id();
                    Map<String, Response<GetPostQuery.Data>> getPostResponses = Posts.query(awsAppSyncClient, appSyncResponseFetcher, postID);

                    if (clientName.equals("API_KEY") || clientName.equals("AWS_IAM")) {
                        assertResponseForQueryPost(appSyncResponseFetcher, getPostResponses, postID);
                    } else if (clientName.startsWith("AMAZON_COGNITO_USER_POOLS")) {
                        assertResponseForQueryPostWithUserPools(appSyncResponseFetcher, getPostResponses, postID);
                    }
                }
            }

            // Clear the cache
            for (final String clientName : appSyncClientMap.keySet()) {
                final AWSAppSyncClient awsAppSyncClient = appSyncClientMap.get(clientName);
                assertNotNull(awsAppSyncClient);
                try {
                    awsAppSyncClient.clearCaches();
                } catch (AWSAppSyncClientException e) {
                    fail("Error in clearing the cache." + e);
                }
            }
        }
    }

    private void assertResponseForQueryPost(ResponseFetcher responseFetcher,
                                   Map<String, Response<GetPostQuery.Data>> responses,
                                   String postID) {
        for (Response<GetPostQuery.Data> response : responses.values()) {
            assertNotNull(response);
            assertNotNull(response.data());
            assertNotNull(response.data().getPost());

            Log.d(TAG, "Response Fetcher: " + responseFetcher + "; postID: " + postID);
            Log.d(TAG, "isFromCache: " + response.fromCache());
            Log.d(TAG, "Post Details: " + response.data().getPost().toString());

            assertNotNull(response.data().getPost().id());
            assertEquals(postID, response.data().getPost().id());
            assertNotNull(response.data().getPost().author());
            assertNotNull(response.data().getPost().content());
            assertNotNull(response.data().getPost().title());
            assertNull(response.data().getPost().url());
            assertNull(response.data().getPost().ups());
            assertNull(response.data().getPost().downs());
        }
    }

    private void assertResponseForQueryPostWithUserPools(ResponseFetcher responseFetcher,
                                            Map<String, Response<GetPostQuery.Data>> responses,
                                            String postID) {
        for (Response<GetPostQuery.Data> response : responses.values()) {
            assertNotNull(response);
            assertNotNull(response.data());
            assertNotNull(response.data().getPost());

            Log.d(TAG, "Response Fetcher: " + responseFetcher + "; postID: " + postID);
            Log.d(TAG, "isFromCache: " + response.fromCache());
            Log.d(TAG, "Post Details: " + response.data().getPost().toString());

            assertNotNull(response.data().getPost().id());
            assertEquals(postID, response.data().getPost().id());
            assertNotNull(response.data().getPost().author());
            assertNotNull(response.data().getPost().content());
            assertNotNull(response.data().getPost().title());
            assertNotNull(response.data().getPost().url());
            assertNotNull(response.data().getPost().ups());
            assertNotNull(response.data().getPost().downs());
        }
    }

    @Test
    public void testQueryAndMutationCacheIsolationWithDifferentAuthModesForPosts() {
        final ResponseFetcher[] appSyncResponseFetchers = new ResponseFetcher[] {
                AppSyncResponseFetchers.NETWORK_ONLY,
                AppSyncResponseFetchers.NETWORK_FIRST
        };

        for (ResponseFetcher appSyncResponseFetcher : appSyncResponseFetchers) {
            Log.d(TAG, "AppSyncResponseFetcher: " + appSyncResponseFetcher.toString());

            final AWSAppSyncClient apiKeyClientForPosts = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();
            final AWSAppSyncClient iamClientForPosts = AWSAppSyncClients.withIAMFromAWSConfiguration();
            final AWSAppSyncClient amazonCognitoUserPoolsClientForPosts = AWSAppSyncClients.withUserPoolsFromAWSConfiguration();

            //Mutate and Query Posts through API Key Client
            Log.d(TAG, "AWSAppSyncClient for API_KEY: " + apiKeyClientForPosts);
            final String title = "testQueryAndMutationCacheIsolationWithDifferentAuthModesForPosts: Learning to Live ";
            final String author = "Dream Theater @ ";
            final String url = "Dream Theater Station";
            final String content = "No energy for anger @" + System.currentTimeMillis();

            final String updatedAuthor = author + System.currentTimeMillis();

            //Add a post through API Key Client
            Response<AddPostMutation.Data> addPostMutationResponse = Posts.add(apiKeyClientForPosts, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            AddPostMutation.CreatePost post = addPostMutationResponse.data().createPost();
            assertNotNull(post);
            String postId = post.id();
            assertNotNull(postId);

            // Update the post through API Key Client
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Kicking off update");
            LatchedGraphQLCallback<UpdatePostMutation.Data> callback = LatchedGraphQLCallback.instance();
            apiKeyClientForPosts.mutate(
                UpdatePostMutation.builder()
                    .input(UpdatePostInput.builder()
                        .id(postId)
                        .author(updatedAuthor)
                        .build())
                    .build(),
                new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
                    "Post", postId, "", "", content, "", 0
                ))
            ).enqueue(callback);

            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Waiting for latches to be counted down");
            Response<UpdatePostMutation.Data> updatePostMutationResponse = callback.awaitSuccessfulResponse();
            assertNotNull(updatePostMutationResponse);
            assertNotNull(updatePostMutationResponse.data());
            assertNotNull(updatePostMutationResponse.data().updatePost());

            Map<String, Response<GetPostQuery.Data>> getPostResponses = Posts.query(apiKeyClientForPosts, appSyncResponseFetcher, postId);
            Response<GetPostQuery.Data> getPostQueryResponse = getPostResponses.get("NETWORK");
            Posts.validate(getPostQueryResponse, postId, "API_KEY");

            // Now, Query the post mutated through IAM Client with CACHE_ONLY and checks if it returns null.
            Log.d(TAG, "AWSAppSyncClient for AWS_IAM: " + iamClientForPosts);
            getPostResponses = Posts.query(iamClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postId);
            getPostQueryResponse = getPostResponses.get("CACHE");

            assertNotNull(getPostQueryResponse);
            assertNull(getPostQueryResponse.data());

            // Now, Query the post mutated through IAM Client with NETWORK_* and checks if it returns valid post object.
            getPostResponses = Posts.query(iamClientForPosts, appSyncResponseFetcher, postId);
            getPostQueryResponse = getPostResponses.get("NETWORK");
            Posts.validate(getPostQueryResponse, postId, "AWS_IAM");


            // Now, Query the post mutated through IAM Client with CACHE_ONLY and checks if it returns valid post object.
            getPostResponses = Posts.query(iamClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postId);
            getPostQueryResponse = getPostResponses.get("CACHE");
            Posts.validate(getPostQueryResponse, postId, "AWS_IAM");


            // Now, Query the post mutated through Amazon Cognito User Pools Client with CACHE_ONLY
            Log.d(TAG, "AWSAppSyncClient for AMAZON_COGNITO_USER_POOLS: " + amazonCognitoUserPoolsClientForPosts);
            getPostResponses = Posts.query(amazonCognitoUserPoolsClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postId);
            getPostQueryResponse = getPostResponses.get("CACHE");

            assertNotNull(getPostQueryResponse);
            assertNull(getPostQueryResponse.data());

            // Now, Query the post mutated through Amazon Cognito User Pools Client with NETWORK_*
            getPostResponses = Posts.query(amazonCognitoUserPoolsClientForPosts, appSyncResponseFetcher, postId);
            getPostQueryResponse = getPostResponses.get("NETWORK");
            Posts.validate(getPostQueryResponse, postId, "AMAZON_COGNITO_USER_POOLS");

            // Now, Query the post mutated through Amazon Cognito User Pools Client with CACHE_ONLY
            getPostResponses = Posts.query(amazonCognitoUserPoolsClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postId);
            getPostQueryResponse = getPostResponses.get("CACHE");
            Posts.validate(getPostQueryResponse, postId, "AMAZON_COGNITO_USER_POOLS");

            try {
                apiKeyClientForPosts.clearCaches();
                iamClientForPosts.clearCaches();
                amazonCognitoUserPoolsClientForPosts.clearCaches();
            } catch (AWSAppSyncClientException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCRUDWithMultipleClientsOfSingleAuthMode() {
        PostCruds.test(Arrays.asList(
            AWSAppSyncClients.withUserPoolsFromAWSConfiguration(),
            AWSAppSyncClients.withUserPools2FromAWSConfiguration(idToken)
        ));
    }

    @Test
    @Ignore("This test needs to be refactored.")
    public void testCRUDWithMultipleClientsAtTheSameTime() {
        List<AWSAppSyncClient> clients = Arrays.asList(
            AWSAppSyncClients.withAPIKEYFromAWSConfiguration(),
            AWSAppSyncClients.withUserPoolsFromAWSConfiguration(),
            AWSAppSyncClients.withIAMFromAWSConfiguration()
        );
        CountDownLatch countDownLatch = new CountDownLatch(clients.size());

        for (AWSAppSyncClient client : clients) {
            new Thread(() -> {
                PostCruds.test(Collections.singletonList(client));
                countDownLatch.countDown();
            }).start();
        }

        Await.latch(countDownLatch);
    }

    /**
     * This test should be run on a physical device or simulator with cellular data turned off.
     * The test disables the wifi on the device to create the offline scenario.
     */
    @Test
    public void testMultipleOfflineMutations() {
        List<AWSAppSyncClient> clients = Arrays.asList(
            AWSAppSyncClients.withAPIKEYFromAWSConfiguration(),
            AWSAppSyncClients.withIAMFromAWSConfiguration()
        );

        for (AWSAppSyncClient client : clients) {
            final String title = "AWSAppSyncMultiClientInstrumentationTest => testMultipleOfflineMutations => Learning to Live ";
            final String author = "Dream Theater @ ";
            final String url = "Dream Theater Station";
            final String content = "No energy for anger @" + System.currentTimeMillis();

            // Add a post
            Response<AddPostMutation.Data> addPostMutationResponse = Posts.add(client, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            AddPostMutation.CreatePost post = addPostMutationResponse.data().createPost();
            assertNotNull(post);
            String postId = post.id();
            assertNotNull(postId);

            int numberOfLatches = 3;
            Wifi.turnOff();

            List<LatchedGraphQLCallback<UpdatePostMutation.Data>> callbacks = new ArrayList<>();
            for (int i = 0; i < numberOfLatches; i++) {
                callbacks.add(LatchedGraphQLCallback.instance());
            }

            for (int index = 0; index < numberOfLatches; index++) {
                Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Kicking off Mutation [" + index + "]");
                client.mutate(
                    UpdatePostMutation.builder()
                        .input(UpdatePostInput.builder()
                            .id(postId)
                            .author(author + index)
                            .build())
                        .build(),
                    new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
                        "Post", postId, "", "", content, "", 0
                    ))
                ).enqueue(callbacks.get(index));
            }

            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Waiting for latches to be counted down");
            for (int index = 0; index < numberOfLatches; index++) {
                Response<UpdatePostMutation.Data> updatePostMutationResponse =
                    callbacks.get(index).awaitSuccessfulResponse();
                assertNotNull(updatePostMutationResponse);
                assertNotNull(updatePostMutationResponse.data());
                assertNotNull(updatePostMutationResponse.data().updatePost());
            }

            Response<GetPostQuery.Data> getPostQueryResponse =
                Posts.query(client, AppSyncResponseFetchers.NETWORK_ONLY, postId).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertEquals(
                author + (numberOfLatches - 1),
                getPostQueryResponse.data().getPost().author()
            );
        }
    }

   /**
    * This test should be run on a physical device or simulator with cellular data turned off.
    * The test disables the wifi on the device to create the offline scenario.
    */
    @Test
    public void testSingleOfflineMutation() {
        List<AWSAppSyncClient> clients = Arrays.asList(
            AWSAppSyncClients.withAPIKEYFromAWSConfiguration(),
            AWSAppSyncClients.withIAMFromAWSConfiguration()
        );

        for (AWSAppSyncClient client : clients) {
            final String title = "Learning to Live ";
            final String author = "Dream Theater @ ";
            final String url = "Dream Theater Station";
            final String content = "No energy for anger @" + System.currentTimeMillis();
            final String updatedAuthor = author + System.currentTimeMillis();

            // Add a post
            Response<AddPostMutation.Data> addPostMutationResponse =
                Posts.add(client, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            AddPostMutation.CreatePost post = addPostMutationResponse.data().createPost();
            assertNotNull(post);
            String postId = post.id();
            assertNotNull(postId);

            Wifi.turnOff();

            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Kicking off update");
            LatchedGraphQLCallback<UpdatePostMutation.Data> callback = LatchedGraphQLCallback.instance();
            client.mutate(
                UpdatePostMutation.builder()
                    .input(UpdatePostInput.builder()
                        .id(postId)
                        .author(updatedAuthor)
                        .build()
                    )
                    .build(),
                new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
                    "Post", postId, "", "", content, "", 0
                ))
            ).enqueue(callback);

            Response<UpdatePostMutation.Data> updatePostMutationResponse = callback.awaitSuccessfulResponse();
            assertNotNull(updatePostMutationResponse);
            assertNotNull(updatePostMutationResponse.data());
            assertNotNull(updatePostMutationResponse.data().updatePost());

            Response<GetPostQuery.Data> getPostQueryResponse =
                Posts.query(client, AppSyncResponseFetchers.NETWORK_ONLY, postId).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertEquals(updatedAuthor, getPostQueryResponse.data().getPost().author());
        }
    }

    @Test
    public void testClearCache() throws ClearCacheException {
        AWSAppSyncClient client = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();
        Wifi.turnOff();

        // Add a post
        AddPostMutation.Data expected = new AddPostMutation.Data(new AddPostMutation.CreatePost(
            "Post", "", "", "", "", "", null, null, 0
        ));
        AddPostMutation addPostMutation = AddPostMutation.builder()
            .input(CreatePostInput.builder()
                .title("Learning to Live ")
                .author("Dream Theater @ ")
                .url("Dream Theater Station")
                .content("No energy for anger @" + System.currentTimeMillis())
                .ups(1)
                .downs(0)
                .build())
            .build();
        client.mutate(addPostMutation, expected).enqueue(NoOpGraphQLCallback.instance());

        assertFalse(client.isMutationQueueEmpty());

        client.clearCaches();

        // Check Mutation Queue
        assertTrue(client.isMutationQueueEmpty());

        // Query from cache and check no data is available
        Response<AllPostsQuery.Data> listPostsResponse =
            Posts.list(client, AppSyncResponseFetchers.CACHE_ONLY).get("CACHE");
        assertNotNull(listPostsResponse);
        assertNull(listPostsResponse.data());
    }

    @Test
    public void testClearCacheWithOptions() throws ClearCacheException {
        AWSAppSyncClient client = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();
        PostCruds.test(Collections.singletonList(client));

        client.clearCaches(ClearCacheOptions.builder()
            .clearQueries()
            .clearMutations()
            .clearSubscriptions()
            .build());

        // Check Mutation Queue
        assertTrue(client.isMutationQueueEmpty());

        // Query from cache and check no data is available
        Response<AllPostsQuery.Data> listPostsResponse =
            Posts.list(client, AppSyncResponseFetchers.CACHE_ONLY).get("CACHE");
        assertNotNull(listPostsResponse);
        assertNull(listPostsResponse.data());
    }

    @Test
    public void testClearMutationsCacheOnly() throws ClearCacheException {
        AWSAppSyncClient client = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();

        Response<AllPostsQuery.Data> networkResponse =
            Posts.list(client, AppSyncResponseFetchers.NETWORK_FIRST)
                .get("NETWORK");
        assertNotNull(networkResponse);
        assertNotNull(networkResponse.data());

        Wifi.turnOff();

        //Add a post
        AddPostMutation.Data addPostMutationData = new AddPostMutation.Data(new AddPostMutation.CreatePost(
            "Post", "", "", "", "", "", null, null, 0
        ));
        AddPostMutation addPostMutation = AddPostMutation.builder()
            .input(CreatePostInput.builder()
                .title("Learning to Live ")
                .author("Dream Theater @ ")
                .url("Dream Theater Station")
                .content("No energy for anger @" + System.currentTimeMillis())
                .ups(1)
                .downs(0)
                .build()
            )
            .build();
        client.mutate(addPostMutation, addPostMutationData).enqueue(NoOpGraphQLCallback.instance());

        // Act: clear the caches.
        client.clearCaches(ClearCacheOptions.builder().clearMutations().build());

        // Check Mutation Queue
        assertTrue(client.isMutationQueueEmpty());

        // Query from cache and check data is available since only mutations queue is cleared.
        Response<AllPostsQuery.Data> cacheResponse =
            Posts.list(client, AppSyncResponseFetchers.CACHE_ONLY).get("CACHE");
        assertNotNull(cacheResponse);
        assertNotNull(cacheResponse.data());
    }

    @Test
    public void testNoClientDatabasePrefixViaAwsConfiguration() {
        // Uses the configuration under the "MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix" configuration key.
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix");
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
            .context(getTargetContext())
            .awsConfiguration(awsConfiguration)
            .build();
        SyncStore.validate(awsAppSyncClient, null);
    }

    @Test
    public void testConfigHasClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        AWSAppSyncClients.validateAppSyncClient(
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .build(),
            JsonExtract.stringValue(appSyncConfig, "ClientDatabasePrefix"),
            JsonExtract.stringValue(appSyncConfig, "AuthMode")
        );
    }

    @Test
    public void testConfigHasClientDatabasePrefixAndUseClientDatabasePrefixFalse() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        AWSAppSyncClients.validateAppSyncClient(
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(false)
                .build(),
            null,
            JsonExtract.stringValue(appSyncConfig, "AuthMode")
        );
    }

    @Test
    public void testConfigHasNoClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix");
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .build()
        );
        assertEquals(
            "ClientDatabasePrefix is not present in AppSync configuration in awsconfiguration.json " +
                "however .useClientDatabasePrefix(true) is passed in.",
            exception.getCause().getLocalizedMessage()
        );
    }

    @Test
    public void testConfigHasNoClientDatabasePrefixAndUseClientDatabasePrefixFalse() throws JSONException {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix");
        AWSAppSyncClients.validateAppSyncClient(
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(false)
                .build(),
            null,
            awsConfiguration
                .optJsonObject("AppSync")
                .getString("AuthMode")
        );
    }

    @Test
    public void testCodeHasClientDatabasePrefixAndUseClientDatabasePrefixTrue() throws JSONException {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        AWSAppSyncClient.Builder awsAppSyncClientBuilder = AWSAppSyncClient.builder()
            .context(getTargetContext())
            .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
            .serverUrl(appSyncConfig.getString("ApiUrl"))
            .region(Regions.fromName(appSyncConfig.getString("Region")))
            .useClientDatabasePrefix(true)
            .clientDatabasePrefix(appSyncConfig.getString("ClientDatabasePrefix"));
        AWSAppSyncClient awsAppSyncClient = awsAppSyncClientBuilder.build();
        AWSAppSyncClients.validateAppSyncClient(
            awsAppSyncClient, appSyncConfig.getString("ClientDatabasePrefix"), appSyncConfig.getString("AuthMode")
        );
    }

    @Test
    public void testCodeHasClientDatabasePrefixAndUseClientDatabasePrefixFalse() throws JSONException {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
            .context(getTargetContext())
            .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
            .serverUrl(appSyncConfig.getString("ApiUrl"))
            .region(Regions.fromName(appSyncConfig.getString("Region")))
            .useClientDatabasePrefix(false)
            .clientDatabasePrefix(appSyncConfig.getString("ClientDatabasePrefix"))
            .build();
        AWSAppSyncClients.validateAppSyncClient(awsAppSyncClient, null, null);
    }

    @Test
    public void testCodeHasNoClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
                .serverUrl(appSyncConfig.getString("ApiUrl"))
                .region(Regions.fromName(appSyncConfig.getString("Region")))
                .useClientDatabasePrefix(true)
                .build()
        );
        String expected = "Please pass in a valid ClientDatabasePrefix when useClientDatabasePrefix is true.";
        assertEquals(expected, exception.getLocalizedMessage());
    }

    @Test
    public void testCodeHasNoClientDatabasePrefixAndUseClientDatabasePrefixFalse() throws JSONException {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
            .context(getTargetContext())
            .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
            .serverUrl(appSyncConfig.getString("ApiUrl"))
            .region(Regions.fromName(appSyncConfig.getString("Region")))
            .useClientDatabasePrefix(false)
            .build();
        AWSAppSyncClients.validateAppSyncClient(
            awsAppSyncClient, null, appSyncConfig.getString("AuthMode")
        );
    }

    @Test
    public void testConfigHasInvalidClientDatabasePrefix() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_InvalidClientDatabasePrefix");
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .build()
        );
        String expected = "ClientDatabasePrefix validation failed. Please pass in characters " +
            "that matches the pattern: ^[_a-zA-Z0-9]+$";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }

    @Test
    public void testCodeHasInvalidClientDatabasePrefix() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
                .serverUrl(appSyncConfig.getString("ApiUrl"))
                .region(Regions.fromName(appSyncConfig.getString("Region")))
                .useClientDatabasePrefix(true)
                .clientDatabasePrefix("MultiAuthAndroidIntegTestApp_!@#$%^&*()") // This is the problem!!
                .build()
        );
        String expected = "ClientDatabasePrefix validation failed. Please pass in characters " +
            "that matches the pattern: ^[_a-zA-Z0-9]+$";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }

    @Test
    public void testCodeHasNullClientDatabasePrefix() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        //noinspection ConstantConditions null argument is what is is being tested
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
                .serverUrl(appSyncConfig.getString("ApiUrl"))
                .region(Regions.fromName(appSyncConfig.getString("Region")))
                .useClientDatabasePrefix(true)
                .clientDatabasePrefix(null) // Note: this is the problem!!
                .build()
        );
        String expected = "Please pass in a valid ClientDatabasePrefix when useClientDatabasePrefix is true.";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }

    @Test
    public void testCodeHasEmptyClientDatabasePrefix() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
                .serverUrl(appSyncConfig.getString("ApiUrl"))
                .region(Regions.fromName(appSyncConfig.getString("Region")))
                .useClientDatabasePrefix(true)
                .clientDatabasePrefix("") // Note: this is the problem!!
                .build()
        );
        String expected = "Please pass in a valid ClientDatabasePrefix when useClientDatabasePrefix is true.";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }

    @Test
    public void testUseSameClientDatabasePrefixForDifferentAuthModes() throws JSONException {
        // Construct client-1 with API_KEY AuthMode and MultiAuthAndroidIntegTestApp_API_KEY prefix
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        AWSAppSyncClient.builder()
            .context(getTargetContext())
            .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
            .serverUrl(appSyncConfig.getString("ApiUrl"))
            .region(Regions.fromName(appSyncConfig.getString("Region")))
            .useClientDatabasePrefix(true)
            .clientDatabasePrefix(appSyncConfig.getString("ClientDatabasePrefix"))
            .build();

        // Construct client-2 with AWS_IAM AuthMode and same prefix ("MultiAuthAndroidIntegTestApp_API_KEY")
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .cognitoUserPoolsAuthProvider(() -> {
                    try {
                        return AWSMobileClient.getInstance()
                            .getTokens()
                            .getIdToken()
                            .toString();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .useClientDatabasePrefix(true)
                .clientDatabasePrefix(appSyncConfig.getString("ClientDatabasePrefix"))
                .build()
        );
        String expected = "ClientDatabasePrefix validation failed. The ClientDatabasePrefix ";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }

    /**
     * Mutation through IAM client has a delay of 15 seconds in fetching the credentials
     * Mutation through API_KEY client has no delay
     * Assert that Mutation through API_KEY should have finished first
     */
    @Test
    public void testMultiClientMutation() {
        CountDownLatch latch = new CountDownLatch(2);
        Map<String, Long> results = new HashMap<>();

        AddPostMutation.Data addPostMutationData = new AddPostMutation.Data(new AddPostMutation.CreatePost(
            "Post", "", "", "", "", "", null, null, 0
        ));
        AddPostMutation addPostMutation = AddPostMutation.builder()
            .input(CreatePostInput.builder()
                .title("Learning to Live ")
                .author("Dream Theater @ ")
                .url("Dream Theater Station")
                .content("No energy for anger @" + System.currentTimeMillis())
                .ups(1)
                .downs(0)
                .build())
            .build();

        AWSAppSyncClients.withIAMFromAWSConfiguration(false, TimeUnit.SECONDS.toMillis(15))
            .mutate(addPostMutation, addPostMutationData)
            .enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
                @Override
                public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                    results.put("AWS_IAM", System.currentTimeMillis());
                    latch.countDown();
                }

                @Override
                public void onFailure(@Nonnull final ApolloException e) {
                    latch.countDown();
                }
            });
        AWSAppSyncClients.withAPIKEYFromAWSConfiguration()
            .mutate(addPostMutation, addPostMutationData)
            .enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
                @Override
                public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                    results.put("API_KEY", System.currentTimeMillis());
                    latch.countDown();
                }

                @Override
                public void onFailure(@Nonnull final ApolloException e) {
                    latch.countDown();
                }
            });
        Await.latch(latch);

        assertTrue(results.size() >= 2);
        Long iamTimeMs = results.get("AWS_IAM");
        Long apiKeyTimeMs = results.get("API_KEY");
        assertNotNull(iamTimeMs);
        assertNotNull(apiKeyTimeMs);
        assertTrue(iamTimeMs > apiKeyTimeMs);
    }

    @Test
    public void testNoContextThrowsException() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .apiKey(new BasicAPIKeyAuthProvider(appSyncConfig.getString("ApiKey")))
                .serverUrl(appSyncConfig.getString("ApiUrl"))
                .region(Regions.fromName(appSyncConfig.getString("Region")))
                .useClientDatabasePrefix(true)
                .clientDatabasePrefix(appSyncConfig.getString("ClientDatabasePrefix"))
                .build()
        );
        String expected = "A valid Android Context is required.";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }

    @Test
    public void testNoAuthModeObjectThrowsExceptionViaCode() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        JSONObject appSyncConfig = awsConfiguration.optJsonObject("AppSync");
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .serverUrl(appSyncConfig.getString("ApiUrl"))
                .region(Regions.fromName(appSyncConfig.getString("Region")))
                .useClientDatabasePrefix(true)
                .clientDatabasePrefix(appSyncConfig.getString("ClientDatabasePrefix"))
                .build()
        );
        String expected = "No valid AuthMode object is passed in to the builder.";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }

    @Test
    public void testNoAuthModeObjectThrowsExceptionViaAWSConfiguration() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            AWSAppSyncClient.builder()
                .context(getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .build()
        );
        String expected = "No valid AuthMode object is passed in to the builder.";
        assertTrue(exception.getLocalizedMessage().startsWith(expected));
    }
}
