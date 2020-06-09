/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.DeletePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.api.Response;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

final class PostCruds {
    private static final String TAG = PostCruds.class.getName();

    static void test(Collection<AWSAppSyncClient> awsAppSyncClients) {
        for (final AWSAppSyncClient awsAppSyncClient : awsAppSyncClients) {
            assertNotNull(awsAppSyncClient);
            final String title = "Home [Scene Six]";
            final String author = "Dream Theater @ " + System.currentTimeMillis();
            final String url = "Metropolis Part 2";
            final String content = "Shine-Lake of fire @" + System.currentTimeMillis();

            Posts.add(awsAppSyncClient, title, author, url, content);
            Posts.addAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            Posts.add(awsAppSyncClient, title, author, url, content);
            Posts.addAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            Posts.add(awsAppSyncClient, title, author, url, content);
            Posts.addAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            Posts.add(awsAppSyncClient, title, author, url, content);
            Posts.addAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            Posts.add(awsAppSyncClient, title, author, url, content);
            Posts.addAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);
            Posts.add(awsAppSyncClient, title, author, url, content);
            Posts.addAndCancel(awsAppSyncClient, title, "" + System.currentTimeMillis(), url, content);

            //Add a post
            Response<AddPostMutation.Data> addPostMutationResponse = Posts.add(awsAppSyncClient, title, author, url, content);
            assertNotNull(addPostMutationResponse);
            assertNotNull(addPostMutationResponse.data());
            AddPostMutation.CreatePost createPost = addPostMutationResponse.data().createPost();
            assertNotNull(createPost);

            String postId = createPost.id();
            assertNotNull(createPost.id());
            Log.d(TAG, "Added Post ID: " + postId);

            //Check that the post has been propagated to the server
            Response<GetPostQuery.Data> getPostQueryResponse = Posts.query(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postId).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertEquals(postId, getPostQueryResponse.data().getPost().id());
            assertEquals(content, getPostQueryResponse.data().getPost().content());

            //Check that the post has been propagated to the server
            getPostQueryResponse = Posts.query(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postId).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertEquals(postId, getPostQueryResponse.data().getPost().id());
            assertEquals(content, getPostQueryResponse.data().getPost().content());


            //Check that the post has been made it to the cache
            getPostQueryResponse = Posts.query(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY, postId).get("CACHE");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertEquals(postId, getPostQueryResponse.data().getPost().id());
            assertEquals(content, getPostQueryResponse.data().getPost().content());


            //Update the Post
            final String updatedContent = "New content coming up @" + System.currentTimeMillis();

            Response<UpdatePostMutation.Data> updatePostMutationResponse = Posts.update(awsAppSyncClient, postId, updatedContent);
            assertNotNull(updatePostMutationResponse);
            assertNotNull(updatePostMutationResponse.data());

            UpdatePostMutation.UpdatePost updatePost = updatePostMutationResponse.data().updatePost();
            assertNotNull(updatePost);
            assertNotNull(updatePost.content());
            assertEquals(updatedContent, updatePost.content());
            assertNotEquals(content, updatePost.content());

            //Check that the information has been propagated to the server
            getPostQueryResponse = Posts.query(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_FIRST, postId).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().id());
            assertEquals(postId, getPostQueryResponse.data().getPost().id());
            assertNotEquals(content, getPostQueryResponse.data().getPost().content());
            assertEquals(updatedContent, getPostQueryResponse.data().getPost().content());

            //Check that the information has been updated in the local cache
            getPostQueryResponse = Posts.query(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY, postId).get("CACHE");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNotNull(getPostQueryResponse.data().getPost());
            assertNotNull(getPostQueryResponse.data().getPost().id());
            assertEquals(postId, getPostQueryResponse.data().getPost().id());
            assertNotEquals(content, getPostQueryResponse.data().getPost().content());
            assertEquals(updatedContent, getPostQueryResponse.data().getPost().content());

            // Delete the Post
            Response<DeletePostMutation.Data> deletePostMutationResponse = Posts.delete(awsAppSyncClient, postId);
            assertNotNull(deletePostMutationResponse);
            assertNotNull(deletePostMutationResponse.data());
            assertNotNull(deletePostMutationResponse.data().deletePost());

            DeletePostMutation.DeletePost deletePost = deletePostMutationResponse.data().deletePost();
            assertNotNull(deletePost);
            assertNotNull(deletePost.id());
            assertEquals(postId, deletePost.id());

            //Check that it is gone from the server
            getPostQueryResponse = Posts.query(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postId).get("NETWORK");
            assertNotNull(getPostQueryResponse);
            assertNotNull(getPostQueryResponse.data());
            assertNull(getPostQueryResponse.data().getPost());
        }
    }
}
