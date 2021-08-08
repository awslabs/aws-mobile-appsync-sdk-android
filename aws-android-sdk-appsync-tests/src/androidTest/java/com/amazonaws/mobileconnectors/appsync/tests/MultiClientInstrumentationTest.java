/*
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.tests;

import static androidx.test.InstrumentationRegistry.getTargetContext;

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
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.identity.CustomCognitoUserPool;
import com.amazonaws.mobileconnectors.appsync.models.Posts;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.util.JsonExtract;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.api.Operation.Variables;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.mobileconnectors.appsync.util.InternetConnectivity.goOffline;
import static com.amazonaws.mobileconnectors.appsync.util.InternetConnectivity.goOnline;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.runner.AndroidJUnit4;

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
    public static void beforeAnyTests() {
        goOnline();
        idToken = CustomCognitoUserPool.setup();
    }

    @Before
    @After
    public void ensureNetworkIsUp() {
        goOnline();
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
    public void testClearCache() throws ClearCacheException {
        AWSAppSyncClient client = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();
        goOffline();

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
    public void testClearMutationsCacheOnly() throws ClearCacheException {
        AWSAppSyncClient client = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();

        Response<AllPostsQuery.Data> networkResponse =
            Posts.list(client, AppSyncResponseFetchers.NETWORK_FIRST)
                .get("NETWORK");
        assertNotNull(networkResponse);
        assertNotNull(networkResponse.data());

        goOffline();

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
}
