/*
 * Copyright 20202 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.demo.AddPostMissingRequiredFieldsMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostRequiredFieldsOnlyMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.DeletePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.DeletePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.fetcher.CacheAndNetworkFetcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
final class Posts {
    private static final String TAG = Posts.class.getName();

    private Posts() {}

    /**
     * Adds a post.
     * @param client AppSync client through which to add post
     * @param title Title of post
     * @param author Author of post
     * @param url URL of post
     * @param content Content of post
     * @return A response from AppSync
     */
    static Response<AddPostMutation.Data> add(
            AWSAppSyncClient client, String title, String author, String url, String content) {
        LatchedGraphQLCallback<AddPostMutation.Data> callback = LatchedGraphQLCallback.instance();
        client.mutate(
            AddPostMutation.builder()
                .input(CreatePostInput.builder()
                    .title(title)
                    .author(author)
                    .url(url)
                    .content(content)
                    .ups(1)
                    .downs(0)
                    .build()
                )
                .build(),
            new AddPostMutation.Data(new AddPostMutation.CreatePost(
                "Post", "", "", "", "", "", null, null, 0
            ))
        )
        .enqueue(callback);
        return callback.awaitResponse();
    }

    /**
     * Deletes a Post, by its ID.
     * @param client AppSnc client through which to perform delete
     * @param postId ID of Post to delete
     * @return A Response from AppSync
     */
    static Response<DeletePostMutation.Data> delete(AWSAppSyncClient client, String postId) {
        LatchedGraphQLCallback<DeletePostMutation.Data> callback = LatchedGraphQLCallback.instance();
        client.mutate(
            DeletePostMutation.builder()
                .input(DeletePostInput.builder()
                    .id(postId)
                    .build()
                )
                .build(),
            new DeletePostMutation.Data(new DeletePostMutation.DeletePost(
                "Post", "", "", "", "", ""
            ))
        ).enqueue(callback);
        return callback.awaitResponse();
    }

    /**
     * Updates an existing Post, by its ID.
     * @param client AppSync client through which to perform update
     * @param postId ID of post being updated
     * @param content New content for post with given ID
     * @return A Response from AppSync
     */
    static Response<UpdatePostMutation.Data> update(
            AWSAppSyncClient client, String postId, String content) {
        LatchedGraphQLCallback<UpdatePostMutation.Data> callback = LatchedGraphQLCallback.instance();
        client.mutate(
            UpdatePostMutation.builder()
                .input(UpdatePostInput.builder()
                    .id(postId)
                    .content(content)
                    .build())
                .build(),
            new UpdatePostMutation.Data( new UpdatePostMutation.UpdatePost(
                "Post", postId, "", "", content, "", 0
            ))
        )
        .enqueue(callback);
        return callback.awaitResponse();
    }

    /**
     * Queries for a post with a given ID.
     * @param client AppSync client through which to make the query
     * @param responseFetcher A fetcher, e.g. {@link CacheAndNetworkFetcher}
     * @param postId ID of a post for which to query
     * @return cached/network responses, labeled in a Map as "NETWORK" and/or "CACHE"
     */
    static Map<String, Response<GetPostQuery.Data>> query(
            AWSAppSyncClient client, ResponseFetcher responseFetcher, String postId) {
        Log.d(TAG, "Calling Query queryPost with responseFetcher: " + responseFetcher.toString());
        int expectedResponseCount =
            responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK) ? 2 : 1;
        CountDownLatch queryCountDownLatch = new CountDownLatch(expectedResponseCount);

        Map<String, Response<GetPostQuery.Data>> getPostQueryResponses = new HashMap<>();
        client.query(GetPostQuery.builder().id(postId).build())
            .responseFetcher(responseFetcher)
            .enqueue(DelegatingGraphQLCallback.to(response -> {
                if (response.fromCache()) {
                    getPostQueryResponses.put("CACHE", response);
                } else {
                    getPostQueryResponses.put("NETWORK", response);
                }
                queryCountDownLatch.countDown();
            }, failure -> {
                failure.printStackTrace();
                queryCountDownLatch.countDown();
            }));
        Await.latch(queryCountDownLatch);

        Log.d(TAG, "responseFetcher: " + responseFetcher.toString() +
            "; Cache response: " + getPostQueryResponses.get("CACHE"));
        Log.d(TAG, "responseFetcher: " + responseFetcher.toString() +
            "; Network response: " + getPostQueryResponses.get("NETWORK"));

        return getPostQueryResponses;
    }

    static Map<String, Response<AllPostsQuery.Data>> list(
            AWSAppSyncClient client, ResponseFetcher responseFetcher) {
        int expectedResponseCount =
            responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK) ? 2 : 1;
        CountDownLatch queryCountDownLatch = new CountDownLatch(expectedResponseCount);

        Log.d(TAG, "Calling Query listPosts with responseFetcher: " + responseFetcher.toString());

        Map<String, Response<AllPostsQuery.Data>> listPostsQueryResponses = new HashMap<>();
        client.query(AllPostsQuery.builder().build())
            .responseFetcher(responseFetcher)
            .enqueue(DelegatingGraphQLCallback.to(
                response -> {
                    if (response.fromCache()) {
                        listPostsQueryResponses.put("CACHE", response);
                    } else {
                        listPostsQueryResponses.put("NETWORK", response);
                    }
                    queryCountDownLatch.countDown();
                }, failure -> {
                    failure.printStackTrace();
                    queryCountDownLatch.countDown();
                }
            ));
        Await.latch(queryCountDownLatch);

        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            assertNotNull(listPostsQueryResponses.get("CACHE"));
            assertNotNull(listPostsQueryResponses.get("NETWORK"));
            assertEquals(2, listPostsQueryResponses.size());
        } else {
            assertEquals(1, listPostsQueryResponses.size());
        }

        return listPostsQueryResponses;
    }

    /**
     * Adds a Post record, but then immediately cancels the request.
     * @param client AppSync Client
     * @param title Title for Post
     * @param author Author of Post
     * @param url URL of Post
     * @param content Content of Post
     */
    static void addAndCancel(
        AWSAppSyncClient client, String title, String author, String url, String content) {
        LatchedGraphQLCallback<AddPostMutation.Data> callback = LatchedGraphQLCallback.instance();

        AppSyncMutationCall<AddPostMutation.Data> call = client.mutate(
            AddPostMutation.builder()
                .input(CreatePostInput.builder()
                    .title(title)
                    .author(author)
                    .url(url)
                    .content(content)
                    .ups(1)
                    .downs(0)
                    .build())
                .build(),
            new AddPostMutation.Data(new AddPostMutation.CreatePost(
                "Post", "", "", "", "", "", null, null, 0
            ))
        );
        call.enqueue(callback);
        Sleep.seconds(1);
        call.cancel();
    }

    static Response<AddPostMissingRequiredFieldsMutation.Data> addMissingRequiredFields(
        AWSAppSyncClient client, String title, String author, String url, String content) {
        LatchedGraphQLCallback<AddPostMissingRequiredFieldsMutation.Data> callback = LatchedGraphQLCallback.instance();
        client.mutate(
            AddPostMissingRequiredFieldsMutation.builder()
                .input(CreatePostInput.builder()
                    .title(title)
                    .author(author)
                    .url(url)
                    .content(content)
                    .ups(1)
                    .downs(0)
                    .build())
                .build(),
            new AddPostMissingRequiredFieldsMutation.Data(null)
        )
            .enqueue(callback);
        return callback.awaitResponse();
    }

    static Response<AddPostRequiredFieldsOnlyMutation.Data> addRequiredFieldsOnly(
        AWSAppSyncClient client, String title, String author, String url, String content) {
        LatchedGraphQLCallback<AddPostRequiredFieldsOnlyMutation.Data> callback =
            LatchedGraphQLCallback.instance();
        client.mutate(
            AddPostRequiredFieldsOnlyMutation.builder()
                .input(CreatePostInput.builder()
                    .title(title)
                    .author(author)
                    .url(url)
                    .content(content)
                    .ups(1)
                    .downs(0)
                    .build()
                )
                .build(),
            new AddPostRequiredFieldsOnlyMutation.Data( null)
        ).enqueue(callback);
        return callback.awaitResponse();
    }

    static void validate(Response<GetPostQuery.Data> response, String postId, String authMode) {
        assertNotNull(response);
        assertNotNull(response.data());
        assertNotNull(response.data().getPost());

        Log.d(TAG, "isFromCache: " + response.fromCache());
        Log.d(TAG, "Post Details: " + response.data().getPost().toString());

        assertNotNull(response.data().getPost().id());
        assertEquals(postId, response.data().getPost().id());
        assertNotNull(response.data().getPost().author());
        assertNotNull(response.data().getPost().content());
        assertNotNull(response.data().getPost().title());

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
}
