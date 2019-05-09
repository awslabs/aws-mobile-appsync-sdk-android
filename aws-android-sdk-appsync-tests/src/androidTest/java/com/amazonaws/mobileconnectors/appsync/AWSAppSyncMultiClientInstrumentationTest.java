/**
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeleteArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeletePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_MUTATION_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_QUERY_SQL_STORE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AWSAppSyncMultiClientInstrumentationTest {
    private static final String TAG = AWSAppSyncMultiClientInstrumentationTest.class.getSimpleName();

    private static AppSyncTestSetupHelper appSyncTestSetupHelper;

    @BeforeClass
    public static void setupOnce() {
        appSyncTestSetupHelper = new AppSyncTestSetupHelper();
    }

    @Test
    public void testSyncOnlyBaseQuery() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration());

        for (AWSAppSyncClient awsAppSyncClient: clients) {
            final CountDownLatch syncLatch = new CountDownLatch(1);

            boolean success = false;
            Query<AllPostsQuery.Data, AllPostsQuery.Data, com.apollographql.apollo.api.Operation.Variables> baseQuery = AllPostsQuery.builder().build();
            GraphQLCall.Callback baseQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
                @Override
                public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                    assertTrue(true);
                    syncLatch.countDown();
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    assertTrue(false);
                    syncLatch.countDown();
                }
            };

            Cancelable handle = awsAppSyncClient.sync(baseQuery, baseQueryCallback, 0);
            assertFalse(handle.isCanceled());
            try {
                syncLatch.await();
            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }

            handle.cancel();
            assertTrue(handle.isCanceled());

            //This should be a NoOp. Test to make sure.
            handle.cancel();
            assertTrue(handle.isCanceled());
        }
    }

    @Test
    public void testSyncOnlyBaseAndDeltaQuery() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration());

        for (AWSAppSyncClient awsAppSyncClient: clients) {
            final CountDownLatch baseQueryLatch = new CountDownLatch(1);
            final CountDownLatch deltaQueryLatch = new CountDownLatch(1);

            Query baseQuery = AllPostsQuery.builder().build();
            GraphQLCall.Callback baseQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
                @Override
                public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                    assertTrue(true);
                    baseQueryLatch.countDown();
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    assertTrue(false);
                    baseQueryLatch.countDown();
                }
            };

            final Query deltaQuery = AllPostsQuery.builder().build();
            GraphQLCall.Callback deltaQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
                @Override
                public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                    assertTrue(true);
                    deltaQueryLatch.countDown();
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    assertTrue(false);
                    deltaQueryLatch.countDown();
                }
            };

            Cancelable handle = awsAppSyncClient.sync(baseQuery, baseQueryCallback, deltaQuery, deltaQueryCallback, 5);
            assertFalse(handle.isCanceled());
            try {
                baseQueryLatch.await(10, TimeUnit.SECONDS);
                deltaQueryLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }
            handle.cancel();
            assertTrue(handle.isCanceled());

            //This should be a No op. Test to make sure that there are no unintended side effects
            handle.cancel();
            assertTrue(handle.isCanceled());
        }
    }

    @Test
    public void testCache() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration());

        for (AWSAppSyncClient awsAppSyncClient : clients) {
            Log.d(TAG, "AWSAppSyncClient: " + awsAppSyncClient);
            assertNotNull(awsAppSyncClient);

            Response<AllPostsQuery.Data> allPostsResponse = appSyncTestSetupHelper.listPosts(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY).get("NETWORK");
            assertNotNull(allPostsResponse);
            assertNotNull(allPostsResponse.data());
            assertNotNull(allPostsResponse.data().listPosts());
            AllPostsQuery.ListPosts listPosts = allPostsResponse.data().listPosts();

            String postID;
            long numPostsFromNetwork = 0;
            for (AllPostsQuery.Item item : listPosts.items()) {
                postID = item.id();
                Response<GetPostQuery.Data> getPostQueryResponse = appSyncTestSetupHelper.queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID).get("NETWORK");
                assertNotNull(getPostQueryResponse);
                assertNotNull(getPostQueryResponse.data());
                assertNotNull(getPostQueryResponse.data().getPost());
                assertEquals(postID, getPostQueryResponse.data().getPost().id());
                numPostsFromNetwork++;
            }

            long numPostsFromCache = 0;
            for (AllPostsQuery.Item item : listPosts.items()) {
                postID = item.id();
                Response<GetPostQuery.Data> getPostQueryResponse = appSyncTestSetupHelper.queryPost(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY, postID).get("CACHE");
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
            appSyncClientMap.put("AMAZON_COGNITO_USER_POOLS", appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration(appSyncResponseFetcher));
            appSyncClientMap.put("AMAZON_COGNITO_USER_POOLS_2", appSyncTestSetupHelper.createAppSyncClientWithUserPools2FromAWSConfiguration(appSyncResponseFetcher));

            for (final String clientName : appSyncClientMap.keySet()) {
                // List Posts
                final AWSAppSyncClient awsAppSyncClient = appSyncClientMap.get(clientName);
                Log.d(TAG, "AWSAppSyncClient for clientName: " + clientName + "; client: " + awsAppSyncClient);
                assertNotNull(awsAppSyncClient);
                Map<String, Response<AllPostsQuery.Data>> listPostsResponses = appSyncTestSetupHelper.listPosts(awsAppSyncClient, appSyncResponseFetcher);
                Response<AllPostsQuery.Data> allPostsResponse = listPostsResponses.get("CACHE") != null
                        ? listPostsResponses.get("CACHE")
                        : listPostsResponses.get("NETWORK");
                assertNotNull(allPostsResponse);
                assertNotNull(allPostsResponse.data());
                AllPostsQuery.ListPosts listPosts = allPostsResponse.data().listPosts();
                assertNotNull(listPosts);

                // Loop over the list and query each post
                String postID;
                for (AllPostsQuery.Item item : listPosts.items()) {
                    postID = item.id();
                    Map<String, Response<GetPostQuery.Data>> getPostResponses = appSyncTestSetupHelper.queryPost(awsAppSyncClient, appSyncResponseFetcher, postID);

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
                    awsAppSyncClient.clearCache();
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
            assertNotNull(response.data().getPost().version());
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
            assertNotNull(response.data().getPost().version());
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

            final AWSAppSyncClient apiKeyClientForPosts = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
            final AWSAppSyncClient iamClientForPosts = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
            final AWSAppSyncClient amazonCognitoUserPoolsClientForPosts = appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration();

            //Mutate and Query Posts through API Key Client
            Log.d(TAG, "AWSAppSyncClient for AWS_IAM: " + apiKeyClientForPosts);
            final String title = "testQueryAndMutationCacheIsolationWithDifferentAuthModesForPosts: Learning to Live ";
            final String author = "Dream Theater @ ";
            final String url = "Dream Theater Station";
            final String content = "No energy for anger @" + System.currentTimeMillis();

            final String updatedAuthor = author + System.currentTimeMillis();

            final CountDownLatch countDownLatch = new CountDownLatch(1);

            //Add a post through API Key Client
            Response<AddPostMutation.Data> addPostMutationResponse = appSyncTestSetupHelper.addPost(apiKeyClientForPosts, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            assertNotNull(addPostMutationResponse.data().createPost());
            assertNotNull(addPostMutationResponse.data().createPost().id());
            String postID = addPostMutationResponse.data().createPost().id();

            //Update the post through API Key Client
            UpdatePostMutation.Data expected = new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
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
                    .author(updatedAuthor)
                    .build();

            final List<Response<UpdatePostMutation.Data>> updatePostMutationResponses = new ArrayList<>();
            UpdatePostMutation updatePostMutation = UpdatePostMutation.builder().input(updatePostInput).build();

            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Kicking off update");
            apiKeyClientForPosts
                    .mutate(updatePostMutation, expected)
                    .enqueue(new GraphQLCall.Callback<UpdatePostMutation.Data>() {
                        @Override
                        public void onResponse(@Nonnull final Response<UpdatePostMutation.Data> response) {
                            updatePostMutationResponses.add(response);
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(@Nonnull final ApolloException e) {
                            Log.d(TAG, "On error called");
                            e.printStackTrace();
                            assertFalse("Update Mutation failed with an exception." + e.getLocalizedMessage(), true);
                            countDownLatch.countDown();

                        }
                    });


            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Waiting for latches to be counted down");
            try {
                assertTrue(countDownLatch.await(60, TimeUnit.SECONDS));
            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }

            Response<UpdatePostMutation.Data> updatePostMutationResponse = updatePostMutationResponses.get(0);
            assertNotNull(updatePostMutationResponse);
            assertNotNull(updatePostMutationResponse.data());
            assertNotNull(updatePostMutationResponse.data().updatePost());

            Map<String, Response<GetPostQuery.Data>> getPostResponses = appSyncTestSetupHelper.queryPost(apiKeyClientForPosts, appSyncResponseFetcher, postID);
            Response<GetPostQuery.Data> getPostQueryResponse = getPostResponses.get("NETWORK");

            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertEquals(updatedAuthor, getPostQueryResponse.data().getPost().author());

            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertNotNull(getPostQueryResponse.data().getPost().content());
            assertNotNull(getPostQueryResponse.data().getPost().title());
            assertNotNull(getPostQueryResponse.data().getPost().version());
            assertNull(getPostQueryResponse.data().getPost().url()); // Protected by AMAZON_COGNITO_USER_POOLS
            assertNull(getPostQueryResponse.data().getPost().ups()); // Protected by AMAZON_COGNITO_USER_POOLS
            assertNull(getPostQueryResponse.data().getPost().downs()); // Protected by AMAZON_COGNITO_USER_POOLS

            // Now, Query the post mutated through IAM Client with CACHE_ONLY and checks if it returns null.
            Log.d(TAG, "AWSAppSyncClient for AWS_IAM: " + iamClientForPosts);
            assertNotNull(iamClientForPosts);

            getPostResponses = appSyncTestSetupHelper.queryPost(iamClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postID);
            getPostQueryResponse = getPostResponses.get("CACHE");

            assertNotNull(getPostQueryResponse);
            assertNull(getPostQueryResponse.data());

            // Now, Query the post mutated through IAM Client with NETWORK_* and checks if it returns valid post object.
            getPostResponses = appSyncTestSetupHelper.queryPost(iamClientForPosts, appSyncResponseFetcher, postID);
            getPostQueryResponse = getPostResponses.get("NETWORK");

            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().id());
            assertEquals(postID, getPostQueryResponse.data().getPost().id());
            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertNotNull(getPostQueryResponse.data().getPost().content());
            assertNotNull(getPostQueryResponse.data().getPost().title());
            assertNotNull(getPostQueryResponse.data().getPost().version());
            assertNull(getPostQueryResponse.data().getPost().url()); // Protected by AMAZON_COGNITO_USER_POOLS
            assertNull(getPostQueryResponse.data().getPost().ups()); // Protected by AMAZON_COGNITO_USER_POOLS
            assertNull(getPostQueryResponse.data().getPost().downs()); // Protected by AMAZON_COGNITO_USER_POOLS

            // Now, Query the post mutated through Amazon Cognito User Pools Client
            Log.d(TAG, "AWSAppSyncClient for AMAZON_COGNITO_USER_POOLS: " + amazonCognitoUserPoolsClientForPosts);
            assertNotNull(amazonCognitoUserPoolsClientForPosts);

            getPostResponses = appSyncTestSetupHelper.queryPost(amazonCognitoUserPoolsClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postID);
            getPostQueryResponse = getPostResponses.get("CACHE");

            assertNotNull(getPostQueryResponse);
            assertNull(getPostQueryResponse.data());

            getPostResponses = appSyncTestSetupHelper.queryPost(amazonCognitoUserPoolsClientForPosts, appSyncResponseFetcher, postID);
            getPostQueryResponse = getPostResponses.get("NETWORK");

            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().id());
            assertEquals(postID, getPostQueryResponse.data().getPost().id());
            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertNotNull(getPostQueryResponse.data().getPost().content());
            assertNotNull(getPostQueryResponse.data().getPost().title());
            assertNotNull(getPostQueryResponse.data().getPost().url());
            assertNotNull(getPostQueryResponse.data().getPost().version());
            assertNotNull(getPostQueryResponse.data().getPost().ups());
            assertNotNull(getPostQueryResponse.data().getPost().downs());

            try {
                apiKeyClientForPosts.clearCache();
                iamClientForPosts.clearCache();
                amazonCognitoUserPoolsClientForPosts.clearCache();
            } catch (AWSAppSyncClientException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCRUDWithMultipleClientsOfSingleAuthMode() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPools2FromAWSConfiguration());
        appSyncTestSetupHelper.testCRUD(clients);
        clients.clear();
    }

    @Test
    public void testCRUDWithSingleClient() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
        appSyncTestSetupHelper.testCRUD(clients);
        clients.clear();
    }

    @Test
    public void testCRUDWithMultipleClients() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration());
        appSyncTestSetupHelper.testCRUD(clients);
    }

    /**
     * This test should be run on a physical device or simulator with cellular data turned off.
     * The test disables the wifi on the device to create the offline scenario.
     */
    @Test
    public void testMultipleOfflineMutations() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration());

        for (AWSAppSyncClient awsAppSyncClient : clients) {
            assertNotNull(awsAppSyncClient);

            final String title = "AWSAppSyncMultiClientInstrumentationTest => testMultipleOfflineMutations => Learning to Live ";
            final String author = "Dream Theater @ ";
            final String url = "Dream Theater Station";
            final String content = "No energy for anger @" + System.currentTimeMillis();

            final int numberOfLatches = 3;
            final CountDownLatch countDownLatches[] = new CountDownLatch[numberOfLatches];
            for (int i = 0; i < numberOfLatches; i++) {
                countDownLatches[i] = new CountDownLatch(1);
            }

            //Add a post
            Response<AddPostMutation.Data> addPostMutationResponse = appSyncTestSetupHelper.addPost(awsAppSyncClient, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            assertNotNull(addPostMutationResponse.data().createPost());
            assertNotNull(addPostMutationResponse.data().createPost().id());
            final String postID = addPostMutationResponse.data().createPost().id();


            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Set Wifi Network offline
                    WifiManager wifiManager = (WifiManager) InstrumentationRegistry.getContext().getSystemService(Context.WIFI_SERVICE);
                    for (int i = 0; i < 3; i++) {
                        assertTrue(wifiManager.setWifiEnabled(false));
                        appSyncTestSetupHelper.sleep((int) (3 * 1000));
                        assertTrue(wifiManager.setWifiEnabled(true));
                        appSyncTestSetupHelper.sleep((int) (7 * 1000));
                    }
                }
            }).start();

            final List<Response<UpdatePostMutation.Data>> updatePostMutationResponses = new ArrayList<>();
            for (int i = 0; i < numberOfLatches; i++) {
                final int position = i;

                UpdatePostMutation.Data expected = new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
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
                        .author(author + position)
                        .build();

                UpdatePostMutation updatePostMutation = UpdatePostMutation.builder().input(updatePostInput).build();

                Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Kicking off Mutation [" + position + "]");
                awsAppSyncClient
                        .mutate(updatePostMutation, expected)
                        .enqueue(new GraphQLCall.Callback<UpdatePostMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<UpdatePostMutation.Data> response) {
                                updatePostMutationResponses.add(response);
                                countDownLatches[position].countDown();
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                Log.d(TAG, "On error called");
                                e.printStackTrace();
                                countDownLatches[position].countDown();

                            }
                        });
            }

            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Waiting for latches to be counted down");
            for (int i = 0; i < numberOfLatches; i++) {
                try {
                    Log.v(TAG, "Waiting on Latch no [" + i + "]");
                    assertTrue(countDownLatches[i].await(60, TimeUnit.SECONDS));
                } catch (InterruptedException iex) {
                    iex.printStackTrace();
                }
            }

            for (int i = 0; i < numberOfLatches; i++) {
                Response<UpdatePostMutation.Data> updatePostMutationResponse = updatePostMutationResponses.get(i);
                assertNotNull(updatePostMutationResponse);
                assertNotNull(updatePostMutationResponse.data());
                assertNotNull(updatePostMutationResponse.data().updatePost());
            }

            Response<GetPostQuery.Data> getPostQueryResponse = appSyncTestSetupHelper.queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertEquals(author + (numberOfLatches - 1), getPostQueryResponse.data().getPost().author());
        }
    }

   /**
    * This test should be run on a physical device or simulator with cellular data turned off.
    * The test disables the wifi on the device to create the offline scenario.
    */
    @Test
    public void testSingleOfflineMutation() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration());

        for (AWSAppSyncClient awsAppSyncClient : clients) {

            assertNotNull(awsAppSyncClient);

            final String title = "Learning to Live ";
            final String author = "Dream Theater @ ";
            final String url = "Dream Theater Station";
            final String content = "No energy for anger @" + System.currentTimeMillis();

            final String updatedAuthor = author + System.currentTimeMillis();

            final CountDownLatch countDownLatch = new CountDownLatch(1);

            //Add a post
            Response<AddPostMutation.Data> addPostMutationResponse = appSyncTestSetupHelper.addPost(awsAppSyncClient, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            assertNotNull(addPostMutationResponse.data().createPost());
            assertNotNull(addPostMutationResponse.data().createPost().id());
            final String postID = addPostMutationResponse.data().createPost().id();


            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Set Wifi Network offline
                    WifiManager wifiManager = (WifiManager) InstrumentationRegistry.getContext().getSystemService(Context.WIFI_SERVICE);
                    assertTrue(wifiManager.setWifiEnabled(false));
                    appSyncTestSetupHelper.sleep(1000);
                    wifiManager.setWifiEnabled(true);
                }
            }).start();

            UpdatePostMutation.Data expected = new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
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
                    .author(updatedAuthor)
                    .build();

            UpdatePostMutation updatePostMutation = UpdatePostMutation.builder().input(updatePostInput).build();

            final List<Response<UpdatePostMutation.Data>> updatePostMutationResponses = new ArrayList<>();
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Kicking off update");
            awsAppSyncClient
                    .mutate(updatePostMutation, expected)
                    .enqueue(new GraphQLCall.Callback<UpdatePostMutation.Data>() {
                        @Override
                        public void onResponse(@Nonnull final Response<UpdatePostMutation.Data> response) {
                            updatePostMutationResponses.add(response);
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(@Nonnull final ApolloException e) {
                            Log.d(TAG, "On error called");
                            e.printStackTrace();
                            countDownLatch.countDown();

                        }
                    });


            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Waiting for latches to be counted down");
            try {
                assertTrue(countDownLatch.await(60, TimeUnit.SECONDS));
            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }

            Response<UpdatePostMutation.Data> updatePostMutationResponse = updatePostMutationResponses.get(0);
            assertNotNull(updatePostMutationResponse);
            assertNotNull(updatePostMutationResponse.data());
            assertNotNull(updatePostMutationResponse.data().updatePost());

            Response<GetPostQuery.Data> getPostQueryResponse = appSyncTestSetupHelper.queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().author());
            assertEquals(updatedAuthor, getPostQueryResponse.data().getPost().author());
        }
    }

    @Test
    public void testMutationQueueEmpty() {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration(true,2*1000);
        assertNotNull(awsAppSyncClient);
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        final CountDownLatch addCountdownlatch = new CountDownLatch(1);

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
                .title("Lonely Day")
                .author("SOAD" + System.currentTimeMillis())
                .url("SOAD.com")
                .content("Such a lonely day")
                .ups(new Integer(1))
                .downs(new Integer(0))
                .build();

        AddPostMutation addPostMutation = AddPostMutation.builder().input(createPostInput).build();
        final AppSyncMutationCall call = awsAppSyncClient.mutate(addPostMutation, expected);

        call.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                call.cancel();
                addCountdownlatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                e.printStackTrace();
                assertTrue("OnError received for Second mutation. Unexpected", false);
                addCountdownlatch.countDown();
            }
        });
        assertFalse(awsAppSyncClient.isMutationQueueEmpty());
        try {
            addCountdownlatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

    }

    @Test
    public void testMutationQueueClear() {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration(true,2*1000);
        assertNotNull(awsAppSyncClient);
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        final int lastMutation = 10;
        final CountDownLatch addCountdownlatch = new CountDownLatch(1);

        for (int i = 0; i < lastMutation; i++ ) {
            final int currentIteration = i;
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
                    .title("Lonely Day")
                    .author("SOAD" + System.currentTimeMillis())
                    .url("SOAD.com")
                    .content("Such a lonely day")
                    .ups(new Integer(1))
                    .downs(new Integer(0))
                    .build();

            AddPostMutation addPostMutation = AddPostMutation.builder().input(createPostInput).build();
            AppSyncMutationCall call = awsAppSyncClient.mutate(addPostMutation, expected);

            call.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
                @Override
                public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                    if (currentIteration == lastMutation) {
                        addCountdownlatch.countDown();
                    }
                }

                @Override
                public void onFailure(@Nonnull final ApolloException e) {
                    if (currentIteration == lastMutation) {
                        addCountdownlatch.countDown();
                    }
                }
            });
        }
        assertFalse(awsAppSyncClient.isMutationQueueEmpty());
        awsAppSyncClient.clearMutationQueue();
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        try {
            assertFalse(addCountdownlatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

    }

    @Test
    public void testClearCache() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        final AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
        clients.add(awsAppSyncClient);
        appSyncTestSetupHelper.testCRUD(clients);

        try {
            awsAppSyncClient.clearCache();
        } catch (Exception ex) {
            fail("Error in clearing cache.");
        }

        // Check Mutation Queue
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        // Query from cache and check no data is available
        Response<AllPostsQuery.Data> listPostsResponse = appSyncTestSetupHelper.listPosts(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY).get("CACHE");
        assertNotNull(listPostsResponse);
        assertNull(listPostsResponse.data());
    }

    @Test
    public void testClearCacheWithOptions() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        final AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
        clients.add(awsAppSyncClient);
        appSyncTestSetupHelper.testCRUD(clients);

        try {
            awsAppSyncClient.clearCache(
                    ClearCacheOptions.builder()
                            .clearQueries()
                            .clearMutations()
                            .clearSubscriptions()
                            .build());
        } catch (Exception ex) {
            fail("Error in clearing cache.");
        }

        // Check Mutation Queue
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        // Query from cache and check no data is available
        Response<AllPostsQuery.Data> listPostsResponse = appSyncTestSetupHelper.listPosts(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY).get("CACHE");
        assertNotNull(listPostsResponse);
        assertNull(listPostsResponse.data());
    }

    @Test
    public void testClearMutationsCacheOnly() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        final AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
        clients.add(awsAppSyncClient);
        appSyncTestSetupHelper.testCRUD(clients);

        // Query from cache and check no data is available
        Response<AllPostsQuery.Data> listPostsResponse = appSyncTestSetupHelper.listPosts(
                awsAppSyncClient,
                AppSyncResponseFetchers.NETWORK_FIRST)
                .get("NETWORK");
        assertNotNull(listPostsResponse);
        assertNotNull(listPostsResponse.data());

        try {
            awsAppSyncClient.clearCache(
                    ClearCacheOptions.builder()
                            .clearMutations()
                            .build());
        } catch (Exception ex) {
            fail("Error in clearing cache.");
        }

        // Check Mutation Queue
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        // Query from cache and check no data is available
        listPostsResponse = appSyncTestSetupHelper.listPosts(
                awsAppSyncClient,
                AppSyncResponseFetchers.CACHE_ONLY)
                .get("CACHE");
        assertNotNull(listPostsResponse);
        assertNotNull(listPostsResponse.data());
    }

    @Test
    public void testNoClientDatabasePrefixViaAwsConfiguration() {
        // Uses the configuration under the "MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix" configuration key.
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix");
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .build();
        assertNotNull(awsAppSyncClient);
        assertNotNull(awsAppSyncClient.mSyncStore);
        assertEquals(DEFAULT_QUERY_SQL_STORE_NAME, awsAppSyncClient.querySqlStoreName);
        assertEquals(DEFAULT_MUTATION_SQL_STORE_NAME, awsAppSyncClient.mutationSqlStoreName);
        assertEquals(DEFAULT_DELTA_SYNC_SQL_STORE_NAME, awsAppSyncClient.deltaSyncSqlStoreName);
    }

    @Test
    public void testConfigHasClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        String clientDatabasePrefixFromConfigJson = null;
        String clientName = null;
        try {
            clientDatabasePrefixFromConfigJson = awsConfiguration
                    .optJsonObject("AppSync")
                    .getString("ClientDatabasePrefix");
            clientName = awsConfiguration
                    .optJsonObject("AppSync")
                    .getString("AuthMode");
        } catch (Exception ex) {
            fail("Error in reading from awsconfiguration.json. " + ex.getLocalizedMessage());
        }

        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(true)
                .build();
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(
                awsAppSyncClient,
                clientDatabasePrefixFromConfigJson,
                clientName);
    }

    @Test
    public void testConfigHasClientDatabasePrefixAndUseClientDatabasePrefixFalse() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        String clientDatabasePrefixFromConfigJson = null;
        String clientName = null;
        try {
            clientDatabasePrefixFromConfigJson = awsConfiguration
                    .optJsonObject("AppSync")
                    .getString("ClientDatabasePrefix");
            clientName = awsConfiguration
                    .optJsonObject("AppSync")
                    .getString("AuthMode");
        } catch (Exception ex) {
            fail("Error in reading from awsconfiguration.json. " + ex.getLocalizedMessage());
        }

        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(false)
                .build();
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(
                awsAppSyncClient,
                null,
                clientName);
    }

    @Test
    public void testConfigHasNoClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix");

        try {
            AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getTargetContext())
                    .awsConfiguration(awsConfiguration)
                    .useClientDatabasePrefix(true)
                    .build();
        } catch (Exception ex) {
            assertNotNull(ex);
            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("ClientDatabasePrefix is not present in AppSync configuration in awsconfiguration.json " +
                    "however .useClientDatabasePrefix(true) is passed in.", ex.getCause().getLocalizedMessage());
        }
    }

    @Test
    public void testConfigHasNoClientDatabasePrefixAndUseClientDatabasePrefixFalse() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());
        awsConfiguration.setConfiguration("MultiAuthAndroidIntegTestApp_NoClientDatabasePrefix");

        String clientName = null;
        try {
            clientName = awsConfiguration
                    .optJsonObject("AppSync")
                    .getString("AuthMode");
        } catch (Exception ex) {
            fail("Error in reading from awsconfiguration.json. " + ex.getLocalizedMessage());
        }

        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .awsConfiguration(awsConfiguration)
                .useClientDatabasePrefix(false)
                .build();
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(
                awsAppSyncClient,
                null,
                clientName);
    }

    @Test
    public void testCodeHasClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());

        String apiKey = null;
        String serverUrl  = null;
        Regions region = null;
        String clientName = null;
        String clientDatabasePrefix = null;

        try {
            apiKey = awsConfiguration.optJsonObject("AppSync").getString("ApiKey");
            serverUrl = awsConfiguration.optJsonObject("AppSync").getString("ApiUrl");
            region = Regions.fromName(awsConfiguration.optJsonObject("AppSync").getString("Region"));
            clientDatabasePrefix = awsConfiguration.optJsonObject("AppSync").getString("ClientDatabasePrefix");
            clientName = awsConfiguration.optJsonObject("AppSync").getString("AuthMode");
        } catch (JSONException e) {
            fail("Error in reading from awsconfiguration.json. " + e.getLocalizedMessage());
        }
        AWSAppSyncClient.Builder awsAppSyncClientBuilder = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .apiKey(new BasicAPIKeyAuthProvider(apiKey))
                .serverUrl(serverUrl)
                .region(region)
                .useClientDatabasePrefix(true)
                .clientDatabasePrefix(clientDatabasePrefix);
        AWSAppSyncClient awsAppSyncClient = awsAppSyncClientBuilder.build();
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(awsAppSyncClient, clientDatabasePrefix, clientName);
    }

    @Test
    public void testCodeHasClientDatabasePrefixAndUseClientDatabasePrefixFalse() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());

        String apiKey = null;
        String serverUrl  = null;
        Regions region = null;
        String clientDatabasePrefix = null;

        try {
            apiKey = awsConfiguration.optJsonObject("AppSync").getString("ApiKey");
            serverUrl = awsConfiguration.optJsonObject("AppSync").getString("ApiUrl");
            region = Regions.fromName(awsConfiguration.optJsonObject("AppSync").getString("Region"));
            clientDatabasePrefix = awsConfiguration.optJsonObject("AppSync").getString("ClientDatabasePrefix");
        } catch (JSONException e) {
            fail("Error in reading from awsconfiguration.json. " + e.getLocalizedMessage());
        }

        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .apiKey(new BasicAPIKeyAuthProvider(apiKey))
                .serverUrl(serverUrl)
                .region(region)
                .useClientDatabasePrefix(false)
                .clientDatabasePrefix(clientDatabasePrefix)
                .build();
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(awsAppSyncClient, null, null);
    }

    @Test
    public void testCodeHasNoClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());

        String apiKey = null;
        String serverUrl  = null;
        Regions region = null;

        try {
            apiKey = awsConfiguration.optJsonObject("AppSync").getString("ApiKey");
            serverUrl = awsConfiguration.optJsonObject("AppSync").getString("ApiUrl");
            region = Regions.fromName(awsConfiguration.optJsonObject("AppSync").getString("Region"));
        } catch (JSONException e) {
            fail("Error in reading from awsconfiguration.json. " + e.getLocalizedMessage());
        }

        try {
            AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getTargetContext())
                    .apiKey(new BasicAPIKeyAuthProvider(apiKey))
                    .serverUrl(serverUrl)
                    .region(region)
                    .useClientDatabasePrefix(true)
                    .build();
        } catch (Exception ex) {
            assertNotNull(ex);
            assertEquals("Please pass in a valid ClientDatabasePrefix when useClientDatabasePrefix is true.", ex.getLocalizedMessage());
        }
    }

    @Test
    public void testCodeHasNoClientDatabasePrefixAndUseClientDatabasePrefixFalse() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(InstrumentationRegistry.getTargetContext());

        String apiKey = null;
        String serverUrl  = null;
        Regions region = null;
        String clientName = null;

        try {
            apiKey = awsConfiguration.optJsonObject("AppSync").getString("ApiKey");
            serverUrl = awsConfiguration.optJsonObject("AppSync").getString("ApiUrl");
            region = Regions.fromName(awsConfiguration.optJsonObject("AppSync").getString("Region"));
            clientName = awsConfiguration.optJsonObject("AppSync").getString("AuthMode");
        } catch (JSONException e) {
            fail("Error in reading from awsconfiguration.json. " + e.getLocalizedMessage());
        }

        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(InstrumentationRegistry.getTargetContext())
                .apiKey(new BasicAPIKeyAuthProvider(apiKey))
                .serverUrl(serverUrl)
                .region(region)
                .useClientDatabasePrefix(false)
                .build();

        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(
                awsAppSyncClient,
                null,
                clientName);
    }
}
