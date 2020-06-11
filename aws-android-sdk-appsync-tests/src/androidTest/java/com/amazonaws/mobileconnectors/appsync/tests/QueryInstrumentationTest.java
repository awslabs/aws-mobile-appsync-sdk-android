/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.tests;

import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncMutationCall;
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
import com.amazonaws.mobileconnectors.appsync.util.Await;
import com.amazonaws.mobileconnectors.appsync.util.Sleep;
import com.amazonaws.mobileconnectors.appsync.util.Wifi;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for base and delta queries, list/get of models.
 */
public final class QueryInstrumentationTest {
    private static final String TAG = QueryInstrumentationTest.class.getSimpleName();
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(10);
    private static final long EXTENDED_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(30);

    @BeforeClass
    public static void beforeAny() {
        CustomCognitoUserPool.setup();
    }

    @Before
    @After
    public void enableWifiIfNotEnabled() {
        Wifi.turnOn();
    }

    @Test
    public void testBaseSyncQuery() {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration();

        // Perform a base query and await success.
        LatchedGraphQLCallback<Operation.Data> baseQueryCallback = LatchedGraphQLCallback.instance();
        Cancelable syncCancelable =
            awsAppSyncClient.sync(AllPostsQuery.builder().build(), baseQueryCallback, 0);
        assertFalse(syncCancelable.isCanceled());
        baseQueryCallback.awaitResponse();

        // Cancel the query. TODO: it just completed with success, so this probably does nothing.
        syncCancelable.cancel();
        assertTrue(syncCancelable.isCanceled());

        // Canceling twice should land things in the same place.
        syncCancelable.cancel();
        assertTrue(syncCancelable.isCanceled());
    }

    @Test
    public void testBaseAndDeltaSyncQuery() {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration();

        AllPostsQuery baseQuery =  AllPostsQuery.builder().build();
        LatchedGraphQLCallback<Operation.Data> baseQueryCallback = LatchedGraphQLCallback.instance();

        AllPostsQuery deltaQuery = AllPostsQuery.builder().build();
        LatchedGraphQLCallback<Operation.Data> deltaQueryCallback = LatchedGraphQLCallback.instance();

        // First, a base sync happens
        Cancelable cancelable = awsAppSyncClient.sync(
            baseQuery,
            baseQueryCallback,
            deltaQuery,
            deltaQueryCallback,
            TimeUnit.HOURS.toSeconds(1)
        );
        assertFalse(cancelable.isCanceled());
        baseQueryCallback.awaitSuccessfulResponse();

        // Next, a delta sync, since the refresh interval isn't over.
        // deltaQueryCallback.awaitResponse(); This does not appear to be working.

        cancelable.cancel();
        assertTrue(cancelable.isCanceled());

        // This should be a No op. Test to make sure that there are no unintended side effects
        cancelable.cancel();
        assertTrue(cancelable.isCanceled());
    }

    @Test
    public void testQueryPostsWithUserPoolsAuthorization() {
        AWSAppSyncClient userPoolsAppSyncClientForPosts = AWSAppSyncClients.withUserPoolsFromAWSConfiguration();
        Log.d(TAG, "AWSAppSyncClient for AMAZON_COGNITO_USER_POOLS: " + userPoolsAppSyncClientForPosts);

        // Query Posts through API Key Client
        Response<AllPostsQuery.Data> allPostsResponse =
            Posts.list(userPoolsAppSyncClientForPosts, AppSyncResponseFetchers.NETWORK_ONLY)
                .get("NETWORK");
        assertNotNull(allPostsResponse);
        assertNotNull(allPostsResponse.data());
        AllPostsQuery.ListPosts listPosts = allPostsResponse.data().listPosts();
        assertNotNull(listPosts);
        List<AllPostsQuery.Item> items = listPosts.items();
        assertNotNull(items);

        // For each of the Posts that were listed, make sure we can
        // query them individually.
        for (AllPostsQuery.Item item : items) {
            String postID = item.id();
            Map<String, Response<GetPostQuery.Data>> responses = Posts.query(
                userPoolsAppSyncClientForPosts,
                AppSyncResponseFetchers.CACHE_AND_NETWORK,
                postID);
            Posts.validate(responses.get("CACHE"), postID, "AMAZON_COGNITO_USER_POOLS");
            Posts.validate(responses.get("NETWORK"), postID, "AMAZON_COGNITO_USER_POOLS");
        }
    }

    @Test
    public void testCRUDWithSingleClient() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(AWSAppSyncClients.withAPIKEYFromAWSConfiguration(false, REASONABLE_WAIT_TIME_MS));
        PostCruds.test(clients);
    }

    /**
     * This test should be run on a physical device or simulator with cellular data turned off.
     * The test disables the wifi on the device to create the offline scenario.
     */
    @Test
    public void testMultipleOfflineMutations() {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withUserPoolsFromAWSConfiguration();

        final String title = "AWSAppSyncMultiClientInstrumentationTest => testMultipleOfflineMutations => Learning to Live ";
        final String author = "Dream Theater @ ";
        final String url = "Dream Theater Station";
        final String content = "No energy for anger @" + System.currentTimeMillis();

        final List<LatchedGraphQLCallback<UpdatePostMutation.Data>> onUpdatePostCallbacks = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            onUpdatePostCallbacks.add(LatchedGraphQLCallback.instance());
        }

        // Add a post
        Response<AddPostMutation.Data> addPostMutationResponse =
            Posts.add(awsAppSyncClient, title, author, url, content);
        assertNotNull(addPostMutationResponse);
        assertNotNull(addPostMutationResponse.data());
        AddPostMutation.CreatePost createPost = addPostMutationResponse.data().createPost();
        assertNotNull(createPost);
        assertNotNull(createPost.id());
        final String postID = createPost.id();

        // Go "offline"
        Wifi.turnOff();

        for (int i = 0; i < onUpdatePostCallbacks.size(); i++) {
            awsAppSyncClient
                .mutate(
                    UpdatePostMutation.builder()
                        .input(UpdatePostInput.builder()
                            .id(postID)
                            .author(author + i)
                            .build())
                        .build(),
                    new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
                        "Post",
                        postID,
                        "",
                        "",
                        content,
                        "",
                        0
                    ))
                )
                .enqueue(onUpdatePostCallbacks.get(i));
        }

        for (int i = 0; i < onUpdatePostCallbacks.size(); i++) {
            Response<UpdatePostMutation.Data> updatePostMutationResponse =
                onUpdatePostCallbacks.get(i).awaitSuccessfulResponse();
            assertNotNull(updatePostMutationResponse);
            assertNotNull(updatePostMutationResponse.data());
            assertNotNull(updatePostMutationResponse.data().updatePost());
        }

        Response<GetPostQuery.Data> getPostQueryResponse =
            Posts.query(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID)
                .get("NETWORK");
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertNotNull(getPostQueryResponse.data().getPost().author());
        assertEquals(author + (onUpdatePostCallbacks.size() - 1), getPostQueryResponse.data().getPost().author());
    }

    /**
     * This test should be run on a physical device or simulator with cellular data turned off.
     * The test disables the wifi on the device to create the offline scenario.
     */
    @Test
    public void testSingleOfflineMutation() {
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration();

        final String title = "AWSAppSyncQueryInstrumentationTest => testSingleOfflineMutation => Learning to Live ";
        final String author = "Dream Theater @ ";
        final String url = "Dream Theater Station";
        final String content = "No energy for anger @" + System.currentTimeMillis();
        final String updatedAuthor = author + System.currentTimeMillis();

        // Add a post
        Response<AddPostMutation.Data> addPostMutationResponse =
            Posts.add(awsAppSyncClient, title, author, url, content);
        assertNotNull(addPostMutationResponse);
        assertNotNull(addPostMutationResponse.data());
        final AddPostMutation.CreatePost createPost = addPostMutationResponse.data().createPost();
        assertNotNull(createPost);
        assertNotNull(createPost.id());
        final String postID = createPost.id();

        // Set Wifi Network offline
        Wifi.turnOff();

        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Kicking off update");
        LatchedGraphQLCallback<UpdatePostMutation.Data> onUpdatePostCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient
            .mutate(
                UpdatePostMutation.builder()
                    .input(UpdatePostInput.builder()
                        .id(postID)
                        .author(updatedAuthor)
                        .build())
                    .build(),
                new UpdatePostMutation.Data(new UpdatePostMutation.UpdatePost(
                    "Post",
                    postID,
                    "",
                    "",
                    content,
                    "",
                    0
                ))
            )
            .enqueue(onUpdatePostCallback);

        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Waiting for latches to be counted down");
        Response<UpdatePostMutation.Data> updatePostMutationResponse = onUpdatePostCallback.awaitResponse();
        assertNotNull(updatePostMutationResponse);
        assertNotNull(updatePostMutationResponse.data());
        assertNotNull(updatePostMutationResponse.data().updatePost());

        Response<GetPostQuery.Data> getPostQueryResponse =
            Posts.query(awsAppSyncClient,
                AppSyncResponseFetchers.NETWORK_ONLY, postID)
                .get("NETWORK");
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertNotNull(getPostQueryResponse.data().getPost().author());
        assertEquals(updatedAuthor, getPostQueryResponse.data().getPost().author());
    }

    @Test
    public void testUpdateWithInvalidID() {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration();

        //Try to update a Post with a Fake ID
        final String updatedContent = "New content coming up @" + System.currentTimeMillis();
        final String randomID = UUID.randomUUID().toString();
        Response<UpdatePostMutation.Data> updatePostMutationResponse =
            Posts.update(awsAppSyncClient, randomID, updatedContent);
        assertNotNull(updatePostMutationResponse);

        UpdatePostMutation.Data data = updatePostMutationResponse.data();
        assertNotNull(data);
        assertNull(data.updatePost());

        assertNotNull(updatePostMutationResponse.errors());
        Error error = updatePostMutationResponse.errors().get(0);
        assertNotNull(error);
        assertNotNull(error.message());
        assertTrue(error.message().contains("The conditional request failed"));
    }

    /**
     * TODO: not clear why this (unusual) edge-case is worth testing.
     */
    @Test
    public  void testCancelMutationWithinCallback() {
        AWSAppSyncClient awsAppSyncClient =
            AWSAppSyncClients.withIAMFromAWSConfiguration();
        final CountDownLatch add2CountDownLatch = new CountDownLatch(1);

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

        CreatePostInput createPostInput1 = CreatePostInput.builder()
            .title("L.A. Woman")
            .author("Doors" + System.currentTimeMillis())
            .url("Doors.com")
            .content("City at night")
            .ups(1)
            .downs(0)
            .build();

        AddPostMutation addPostMutation1 = AddPostMutation.builder().input(createPostInput1).build();
        final AppSyncMutationCall<AddPostMutation.Data> call1 =
            awsAppSyncClient.mutate(addPostMutation1, expected);

        CreatePostInput createPostInput2 = CreatePostInput.builder()
            .title("Break On Through")
            .author("Doors" + System.currentTimeMillis())
            .url("Doors.com")
            .content("To the other side")
            .ups(1)
            .downs(0)
            .build();
        AddPostMutation addPostMutation2 = AddPostMutation.builder().input(createPostInput2).build();
        final AppSyncMutationCall<AddPostMutation.Data> call2 =
            awsAppSyncClient.mutate(addPostMutation2, expected);

        call1.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
            @Override
            public void onResponse(@NonNull Response<AddPostMutation.Data> response) {
                call1.cancel();
            }

            @Override
            public void onFailure(@NonNull ApolloException e) {
                fail("OnError received for first mutation. Unexpected exception: " + e.getMessage());
            }
        });

        call2.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
            @Override
            public void onResponse(@NonNull Response<AddPostMutation.Data> response) {
                call2.cancel();
                add2CountDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull ApolloException e) {
                fail("OnError received for Second mutation. Unexpected exception: " + e.getMessage());
                add2CountDownLatch.countDown();
            }
        });

        Await.latch(add2CountDownLatch, REASONABLE_WAIT_TIME_MS);
    }

    @Test
    public void mutationQueueIsEmptyAfterMutationCompletes() {
        AWSAppSyncClient awsAppSyncClient =
            AWSAppSyncClients.withIAMFromAWSConfiguration(true, REASONABLE_WAIT_TIME_MS);
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        // Note: when the test starts, we assume the mutation queue is going to be empty.

        // Act: Put something in the queue - this AddPostMutation.
        LatchedGraphQLCallback<AddPostMutation.Data> addPostCallback = LatchedGraphQLCallback.instance(EXTENDED_WAIT_TIME_MS);
        awsAppSyncClient.mutate(
            AddPostMutation.builder()
                .input(CreatePostInput.builder()
                    .title("Lonely Day")
                    .author("SOAD" + System.currentTimeMillis())
                    .url("SOAD.com")
                    .content("Such a lonely day")
                    .ups(1)
                    .downs(0)
                    .build())
                .build(),
            new AddPostMutation.Data(new AddPostMutation.CreatePost(
                "Post",
                "",
                "",
                "",
                "",
                "",
                null,
                null,
                0
            ))
        )
        .enqueue(addPostCallback);

        // At first, the mutation is enqueued.
        assertFalse(awsAppSyncClient.isMutationQueueEmpty());

        // Once it executes, though, it's no longer in the queue.
        addPostCallback.awaitResponse();
        Sleep.milliseconds(REASONABLE_WAIT_TIME_MS); // Adding a little buffer ...
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());
    }

    /**
     * When the mutation queue is populated with some mutations, and then
     * {@link AWSAppSyncClient#clearMutationQueue()} is called, the mutation
     * queue should be emptied without executing the mutations.
     */
    @Test
    public void mutationQueueCanBeCleared() {
        AWSAppSyncClient awsAppSyncClient =
            AWSAppSyncClients.withIAMFromAWSConfiguration(true, TimeUnit.SECONDS.toMillis(2));
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());

        // Queue up "a bunch" of mutations. 10 is arbitrary chosen as "a bunch."
        // The type of mutation does not matter for this test. AddPostMutation are nearby and available,
        // so they'll work.
        for (int i = 0; i < 10; i++ ) {
            awsAppSyncClient.mutate(
                AddPostMutation.builder()
                    .input(CreatePostInput.builder()
                        .title("Lonely Day")
                        .author("SOAD" + System.currentTimeMillis())
                        .url("SOAD.com")
                        .content("Such a lonely day")
                        .ups(1)
                        .downs(0)
                        .build())
                    .build(),
                new AddPostMutation.Data(new AddPostMutation.CreatePost(
                    "Post",
                    "",
                    "",
                    "",
                    "",
                    "",
                    null,
                    null,
                    0
                ))
            )
            // This test doesn't actually wait for completion, we're just going
            // to inspect that something is _queued_ to be executed.
            // So, we don't care about what arrives on the callback.
            .enqueue(NoOpGraphQLCallback.instance());
        }
        assertFalse(awsAppSyncClient.isMutationQueueEmpty());
        //noinspection deprecation TODO: @deprecated, but SDK does not propose migration path.
        awsAppSyncClient.clearMutationQueue();
        assertTrue(awsAppSyncClient.isMutationQueueEmpty());
    }
}
