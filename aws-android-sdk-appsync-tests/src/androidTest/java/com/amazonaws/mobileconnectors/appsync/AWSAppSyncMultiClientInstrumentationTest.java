/**
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.GraphQLCall;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_MUTATION_SQL_STORE_NAME;
import static com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient.DEFAULT_QUERY_SQL_STORE_NAME;
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
public class AWSAppSyncMultiClientInstrumentationTest {
    private static final String TAG = AWSAppSyncMultiClientInstrumentationTest.class.getSimpleName();

    private static AppSyncTestSetupHelper appSyncTestSetupHelper;

    @BeforeClass
    public static void setupOnce() {
        appSyncTestSetupHelper = new AppSyncTestSetupHelper();
    }

    @Before
    @After
    public void checkNetworkIsOnline() {
        WifiManager wifiManager = (WifiManager) InstrumentationRegistry.getContext().getSystemService(Context.WIFI_SERVICE);
        if (WIFI_STATE_ENABLED != wifiManager.getWifiState()) {
            assertTrue(wifiManager.setWifiEnabled(true));
            Sleep.seconds(3);
        }
        assert(wifiManager.setWifiEnabled(true));
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

    /**
     * Call sync and get a Base query callback
     * Call sync and get a Delta query callback
     * Call clearCaches to clear the delta sync store
     * Call sync and get a Base query callback
     */
    @Test
    public void testClearDeltaSyncStore() {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
        assertNotNull(awsAppSyncClient);

        final CountDownLatch baseQueryLatch = new CountDownLatch(1);
        Query baseQuery = AllPostsQuery.builder().build();
        GraphQLCall.Callback baseQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                baseQueryLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                fail("BaseQuery failed with an exception");
                baseQueryLatch.countDown();
            }
        };

        final Query deltaQuery = AllPostsQuery.builder().build();
        GraphQLCall.Callback deltaQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                fail("DeltaQuery failed with an exception");
            }
        };

        Cancelable firstSyncHandle = awsAppSyncClient.sync(baseQuery,
                baseQueryCallback,
                deltaQuery,
                deltaQueryCallback,
                5);

        assertFalse(firstSyncHandle.isCanceled());
        try {
            baseQueryLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        final CountDownLatch deltaQueryLatch = new CountDownLatch(1);
        baseQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                fail("BaseQuery failed with an exception");
            }
        };

        deltaQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                deltaQueryLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                deltaQueryLatch.countDown();
                fail("DeltaQuery failed with an exception");
            }
        };

        Cancelable secondSyncHandle = awsAppSyncClient.sync(baseQuery,
                baseQueryCallback,
                deltaQuery,
                deltaQueryCallback,
                5);

        assertFalse(secondSyncHandle.isCanceled());
        try {
            deltaQueryLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        secondSyncHandle.cancel();
        assertTrue(secondSyncHandle.isCanceled());

        firstSyncHandle.cancel();
        assertTrue(firstSyncHandle.isCanceled());

        try {
            awsAppSyncClient.clearCaches();
        } catch (AWSAppSyncClientException e) {
            e.printStackTrace();
        }

        final CountDownLatch baseQueryLatchAfterClearCaches = new CountDownLatch(1);
        baseQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                baseQueryLatchAfterClearCaches.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                fail("BaseQuery failed with an exception");
                baseQueryLatchAfterClearCaches.countDown();
            }
        };

        deltaQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                fail("DeltaQuery failed with an exception");
            }
        };

        Cancelable baseQueryHandleAfterClearCaches = awsAppSyncClient.sync(baseQuery,
                baseQueryCallback,
                deltaQuery,
                deltaQueryCallback,
                5);

        assertFalse(baseQueryHandleAfterClearCaches.isCanceled());
        try {
            baseQueryLatchAfterClearCaches.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        baseQueryHandleAfterClearCaches.cancel();
        assertTrue(baseQueryHandleAfterClearCaches.isCanceled());
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
            Log.d(TAG, "AWSAppSyncClient for API_KEY: " + apiKeyClientForPosts);
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
            appSyncTestSetupHelper.assertQueryPostResponse(getPostQueryResponse, postID, "API_KEY");

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
            appSyncTestSetupHelper.assertQueryPostResponse(getPostQueryResponse, postID, "AWS_IAM");


            // Now, Query the post mutated through IAM Client with CACHE_ONLY and checks if it returns valid post object.
            getPostResponses = appSyncTestSetupHelper.queryPost(iamClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postID);
            getPostQueryResponse = getPostResponses.get("CACHE");
            appSyncTestSetupHelper.assertQueryPostResponse(getPostQueryResponse, postID, "AWS_IAM");


            // Now, Query the post mutated through Amazon Cognito User Pools Client with CACHE_ONLY
            Log.d(TAG, "AWSAppSyncClient for AMAZON_COGNITO_USER_POOLS: " + amazonCognitoUserPoolsClientForPosts);
            assertNotNull(amazonCognitoUserPoolsClientForPosts);

            getPostResponses = appSyncTestSetupHelper.queryPost(amazonCognitoUserPoolsClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postID);
            getPostQueryResponse = getPostResponses.get("CACHE");

            assertNotNull(getPostQueryResponse);
            assertNull(getPostQueryResponse.data());

            // Now, Query the post mutated through Amazon Cognito User Pools Client with NETWORK_*
            getPostResponses = appSyncTestSetupHelper.queryPost(amazonCognitoUserPoolsClientForPosts, appSyncResponseFetcher, postID);
            getPostQueryResponse = getPostResponses.get("NETWORK");
            appSyncTestSetupHelper.assertQueryPostResponse(getPostQueryResponse, postID, "AMAZON_COGNITO_USER_POOLS");

            // Now, Query the post mutated through Amazon Cognito User Pools Client with CACHE_ONLY
            getPostResponses = appSyncTestSetupHelper.queryPost(amazonCognitoUserPoolsClientForPosts, AppSyncResponseFetchers.CACHE_ONLY, postID);
            getPostQueryResponse = getPostResponses.get("CACHE");
            appSyncTestSetupHelper.assertQueryPostResponse(getPostQueryResponse, postID, "AMAZON_COGNITO_USER_POOLS");

            try {
                apiKeyClientForPosts.clearCaches();
                iamClientForPosts.clearCaches();
                amazonCognitoUserPoolsClientForPosts.clearCaches();
            } catch (AWSAppSyncClientException e) {
                e.printStackTrace();
            }
        }
    }

    @Suppress
    @Test
    public void testCRUDWithMultipleClientsOfSingleAuthMode() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration());
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPools2FromAWSConfiguration());
        appSyncTestSetupHelper.testCRUD(clients);
        clients.clear();
    }

    @Test
    public void testCRUDWithMultipleClientsAtTheSameTime() {
        final CountDownLatch countDownLatch = new CountDownLatch(3);

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AWSAppSyncClient> clients = new ArrayList<>();
                clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration());
                appSyncTestSetupHelper.testCRUD(clients);
                countDownLatch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AWSAppSyncClient> clients = new ArrayList<>();
                clients.add(appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration());
                appSyncTestSetupHelper.testCRUD(clients);
                countDownLatch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AWSAppSyncClient> clients = new ArrayList<>();
                clients.add(appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration());
                appSyncTestSetupHelper.testCRUD(clients);
                countDownLatch.countDown();
            }
        }).start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                        Sleep.seconds(3);
                        assertTrue(wifiManager.setWifiEnabled(true));
                        Sleep.seconds(7);
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

        for (AWSAppSyncClient awsAppSyncClient: clients) {

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
                    Sleep.seconds(1);
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
    public void testClearCache() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        final AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
        clients.add(awsAppSyncClient);

        final String title = "Learning to Live ";
        final String author = "Dream Theater @ ";
        final String url = "Dream Theater Station";
        final String content = "No energy for anger @" + System.currentTimeMillis();

        //Set Wifi Network offline
        WifiManager wifiManager = (WifiManager) InstrumentationRegistry.getContext().getSystemService(Context.WIFI_SERVICE);
        assertTrue(wifiManager.setWifiEnabled(false));
        Sleep.seconds(3);

        //Add a post
        final CountDownLatch countDownLatch = new CountDownLatch(1);
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
                if (Looper.myLooper() != null) {
                    Looper.myLooper().quit();
                }
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                e.printStackTrace();
                if (Looper.myLooper() != null) {
                    Looper.myLooper().quit();
                }
                countDownLatch.countDown();
            }
        });

        assertFalse(awsAppSyncClient.isMutationQueueEmpty());

        try {
            awsAppSyncClient.clearCaches();
        } catch (Exception ex) {
            fail("Error in clearing cache.");
        }

        // Check Mutation Queue
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        // Query from cache and check no data is available
        Response<AllPostsQuery.Data> listPostsResponse = appSyncTestSetupHelper.listPosts(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY).get("CACHE");
        assertNotNull(listPostsResponse);
        assertNull(listPostsResponse.data());

        //Set Wifi Network online
        assertTrue(wifiManager.setWifiEnabled(true));
        Sleep.seconds(3);
    }

    @Test
    public void testClearCacheWithOptions() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        final AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
        clients.add(awsAppSyncClient);
        appSyncTestSetupHelper.testCRUD(clients);

        try {
            awsAppSyncClient.clearCaches(
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

        // Query from cache and check no data is available
        Response<AllPostsQuery.Data> listPostsResponse = appSyncTestSetupHelper.listPosts(
                awsAppSyncClient,
                AppSyncResponseFetchers.NETWORK_FIRST)
                .get("NETWORK");
        assertNotNull(listPostsResponse);
        assertNotNull(listPostsResponse.data());

        final String title = "Learning to Live ";
        final String author = "Dream Theater @ ";
        final String url = "Dream Theater Station";
        final String content = "No energy for anger @" + System.currentTimeMillis();

        //Set Wifi Network offline
        WifiManager wifiManager = (WifiManager) InstrumentationRegistry.getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        Sleep.seconds(3);

        //Add a post
        final CountDownLatch countDownLatch = new CountDownLatch(1);
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
                if (Looper.myLooper() != null) {
                    Looper.myLooper().quit();
                }
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                e.printStackTrace();
                if (Looper.myLooper() != null) {
                    Looper.myLooper().quit();
                }
                countDownLatch.countDown();
            }
        });

        assertFalse(awsAppSyncClient.isMutationQueueEmpty());

        try {
            awsAppSyncClient.clearCaches(ClearCacheOptions.builder().clearMutations().build());
        } catch (Exception ex) {
            fail("Error in clearing cache.");
        }

        // Check Mutation Queue
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        // Query from cache and check data is available since only mutations queue is cleared.
        listPostsResponse = appSyncTestSetupHelper.listPosts(
                awsAppSyncClient,
                AppSyncResponseFetchers.CACHE_ONLY)
                .get("CACHE");
        assertNotNull(listPostsResponse);
        assertNotNull(listPostsResponse.data());

        //Set Wifi Network online
        wifiManager.setWifiEnabled(true);
        Sleep.seconds(3);
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
        assertNotNull(awsAppSyncClient);
        assertNotNull(awsAppSyncClient.mSyncStore);
        assertEquals(DEFAULT_QUERY_SQL_STORE_NAME, awsAppSyncClient.querySqlStoreName);
        assertEquals(DEFAULT_MUTATION_SQL_STORE_NAME, awsAppSyncClient.mutationSqlStoreName);
        assertEquals(DEFAULT_DELTA_SYNC_SQL_STORE_NAME, awsAppSyncClient.deltaSyncSqlStoreName);
    }

    @Test
    public void testConfigHasClientDatabasePrefixAndUseClientDatabasePrefixTrue() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
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
                .context(getTargetContext())
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
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
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
                .context(getTargetContext())
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
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(
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
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(
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
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(awsAppSyncClient, null, null);
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
        appSyncTestSetupHelper.assertAWSAppSynClientObjectConstruction(
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
        AWSAppSyncClient iamClientWithDelay = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration(false, 15 * 1000);
        AWSAppSyncClient apiKeyClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();

        final Map<String, Long> resultMap = new HashMap<>();

        //Add a post
        final String title = "Learning to Live ";
        final String author = "Dream Theater @ ";
        final String url = "Dream Theater Station";
        final String content = "No energy for anger @" + System.currentTimeMillis();

        final CountDownLatch mCountDownLatch = new CountDownLatch(2);
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
        AppSyncMutationCall call = iamClientWithDelay.mutate(addPostMutation, expected);
        call.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                resultMap.put("AWS_IAM", System.currentTimeMillis());
                mCountDownLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                mCountDownLatch.countDown();
            }
        });

        AppSyncMutationCall callWithAPiKeyClient = apiKeyClient.mutate(addPostMutation, expected);
        callWithAPiKeyClient.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                resultMap.put("API_KEY", System.currentTimeMillis());
                mCountDownLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                mCountDownLatch.countDown();
            }
        });

        try {
            mCountDownLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(resultMap.size() >= 2);
        assertNotNull(resultMap.get("AWS_IAM"));
        assertNotNull(resultMap.get("API_KEY"));
        assertTrue(resultMap.get("AWS_IAM") > resultMap.get("API_KEY"));
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
