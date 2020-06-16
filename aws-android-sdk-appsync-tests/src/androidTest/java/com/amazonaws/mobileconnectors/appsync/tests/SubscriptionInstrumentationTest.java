/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.tests;

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.client.AWSAppSyncClients;
import com.amazonaws.mobileconnectors.appsync.client.LatchedSubscriptionCallback;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeleteArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeletePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.identity.CustomCognitoUserPool;
import com.amazonaws.mobileconnectors.appsync.models.Posts;
import com.amazonaws.mobileconnectors.appsync.util.Sleep;
import com.apollographql.apollo.api.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.amazonaws.mobileconnectors.appsync.util.InternetConnectivity.goOnline;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class SubscriptionInstrumentationTest {
    private static final String TAG = SubscriptionInstrumentationTest.class.getSimpleName();
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(10);

    @BeforeClass
    public static void beforeAnyTests() {
        goOnline();
        CustomCognitoUserPool.setup();
    }


    private static void testMultipleSubscriptionsWithIAM(SubscriptionReconnectMode subscriptionReconnectMode) {
        final boolean shouldAutomaticallyReconnect =
            SubscriptionReconnectMode.AUTOMATICALLY_RECONNECT.equals(subscriptionReconnectMode);
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration(shouldAutomaticallyReconnect, 0);

        // TODO: why is this looped over 3 times?
        for (int iteration = 0 ; iteration < 3; iteration ++ ) {
            final String title = "Pull Me Under";
            final String author = "Dream Theater @ " + System.currentTimeMillis();
            final String url = "Dream Theater";
            final String content = "Lost in the sky @" + System.currentTimeMillis();

            // Form subscription to creations of Post models.
            AppSyncSubscriptionCall<OnCreatePostSubscription.Data> onCreatePostSubscriptionCall =
                awsAppSyncClient.subscribe(OnCreatePostSubscription.builder().build());
            LatchedSubscriptionCallback<OnCreatePostSubscription.Data> onCreatePostCallback =
                LatchedSubscriptionCallback.instance();
            onCreatePostSubscriptionCall.execute(onCreatePostCallback);

            // Form a subscription to updates of Post models.
            AppSyncSubscriptionCall<OnUpdatePostSubscription.Data> onUpdatePostSubscriptionCall =
                awsAppSyncClient.subscribe(OnUpdatePostSubscription.builder().build());
            LatchedSubscriptionCallback<OnUpdatePostSubscription.Data> onUpdatePostCallback =
                LatchedSubscriptionCallback.instance();
            onUpdatePostSubscriptionCall.execute(onUpdatePostCallback);

            // Form a subscription to deletions of Post models.
            AppSyncSubscriptionCall<OnDeletePostSubscription.Data> onDeletePostSubscriptionCall =
                awsAppSyncClient.subscribe(OnDeletePostSubscription.builder().build());
            LatchedSubscriptionCallback<OnDeletePostSubscription.Data> onDeletePostCallback =
                LatchedSubscriptionCallback.instance();
            onDeletePostSubscriptionCall.execute(onDeletePostCallback);

            // Form a subscription to creations of Articles.
            AppSyncSubscriptionCall<OnCreateArticleSubscription.Data> onCreateArticleSubscriptionCall =
                awsAppSyncClient.subscribe(OnCreateArticleSubscription.builder().build());
            LatchedSubscriptionCallback<OnCreateArticleSubscription.Data> onCreateArticleCallback =
                LatchedSubscriptionCallback.instance();
            onCreateArticleSubscriptionCall.execute(onCreateArticleCallback);

            // Form a subscription to update to Articles.
            AppSyncSubscriptionCall<OnUpdateArticleSubscription.Data> onUpdateArticleSubscriptionCall =
                awsAppSyncClient.subscribe(OnUpdateArticleSubscription.builder().build());
            LatchedSubscriptionCallback<OnUpdateArticleSubscription.Data> onUpdateArticleCallback =
                LatchedSubscriptionCallback.instance();
            onUpdateArticleSubscriptionCall.execute(onUpdateArticleCallback);

            // Form a subscription to deletions of Articles.
            OnDeleteArticleSubscription onDeleteArticleSubscription = OnDeleteArticleSubscription.builder().build();
            AppSyncSubscriptionCall<OnDeleteArticleSubscription.Data>  onDeleteArticleSubscriptionCall =
                awsAppSyncClient.subscribe(onDeleteArticleSubscription);
            LatchedSubscriptionCallback<OnDeleteArticleSubscription.Data> onDeleteArticleCallback =
                LatchedSubscriptionCallback.instance();
            onDeleteArticleSubscriptionCall.execute(onDeleteArticleCallback);

            // *Assume* that all those subscriptions will be active after waiting a while ...
            Sleep.milliseconds(REASONABLE_WAIT_TIME_MS);
            Log.d(TAG, "Subscribed and setup callback handlers.");

            // Create a Post.
            Response<AddPostMutation.Data> addPostMutationResponse =
                Posts.add(awsAppSyncClient, title, author, url, content);
            assertNotNull(addPostMutationResponse.data());

            AddPostMutation.CreatePost createPost = addPostMutationResponse.data().createPost();
            assertNotNull(createPost);

            String postId = createPost.id();
            Log.d(TAG, "Added Post with ID = " + postId);

            // Update that same post, 5 times.
            for (int i = 0; i < 5; i++) {
                Response<UpdatePostMutation.Data> updatePostMutationResponse = Posts.update(
                    awsAppSyncClient,
                    postId,
                    "Lost in the sky @" + System.currentTimeMillis());
                assertNotNull(updatePostMutationResponse);
            }
            Log.d(TAG, "Updated post five times");

            // Okay, now delete the post.
            Posts.delete(awsAppSyncClient, postId);
            Log.d(TAG, "Deleted post");

            // Validate that the mutations "worked".
            onCreatePostCallback.awaitNextSuccessfulResponse();
            onUpdatePostCallback.awaitSuccessfulResponses(5);
            onDeletePostCallback.awaitNextSuccessfulResponse();

            // Cancel all ongoing subscription calls.
            onCreatePostSubscriptionCall.cancel();
            onUpdatePostSubscriptionCall.cancel();
            onDeletePostSubscriptionCall.cancel();
            onCreateArticleSubscriptionCall.cancel();
            onUpdateArticleSubscriptionCall.cancel();
            onDeleteArticleSubscriptionCall.cancel();

            // In response to canceling the calls, the callbacks should receive
            // completion callbacks.
            onCreatePostCallback.awaitCompletion();
            onUpdatePostCallback.awaitCompletion();
            onDeletePostCallback.awaitCompletion();
            onCreateArticleCallback.awaitCompletion();
            onUpdateArticleCallback.awaitCompletion();
            onDeleteArticleCallback.awaitCompletion();
        }
    }

    @Test
    public void testAddSubscriptionWithApiKeyAuthModel() {
        testAddSubscriptionWithApiKeyAuthModel(SubscriptionReconnectMode.AUTOMATICALLY_RECONNECT);
    }

    @Test
    public void testAddSubscriptionWithApiKeyAuthModelNoReconnect() {
        testAddSubscriptionWithApiKeyAuthModel(SubscriptionReconnectMode.STAY_DISCONNECTED);
    }

    private static void testAddSubscriptionWithApiKeyAuthModel(SubscriptionReconnectMode subscriptionReconnectMode) {
        boolean shouldAutomaticallyReconnect =
            SubscriptionReconnectMode.AUTOMATICALLY_RECONNECT.equals(subscriptionReconnectMode);
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration(shouldAutomaticallyReconnect, 0);

        final String title = "Alabama Song [Whisky Bar]";
        final String author = "Doors @ " + System.currentTimeMillis();
        final String url = "The Doors";
        final String content = "Well, show me the way, to the next whisky bar @" + System.currentTimeMillis();

        // Subscribe to creations of Post.
        AppSyncSubscriptionCall<OnCreatePostSubscription.Data> onCreatePostSubscriptionCall =
            awsAppSyncClient.subscribe(OnCreatePostSubscription.builder().build());
        LatchedSubscriptionCallback<OnCreatePostSubscription.Data> onCreatePostCallback =
            LatchedSubscriptionCallback.instance();
        onCreatePostSubscriptionCall.execute(onCreatePostCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        // Sleep for a while to make sure the subscription goes through
        Sleep.milliseconds(REASONABLE_WAIT_TIME_MS);

        Posts.add(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post");

        // Did the post show up on the subscription?
        onCreatePostCallback.awaitNextSuccessfulResponse();

        // Cancel the subscription call, and expect a completion callback.
        onCreatePostSubscriptionCall.cancel();
        onCreatePostCallback.awaitCompletion();
    }

    @Test
    public void testAddSubscriptionWithIAMAuthModelForNullPatching() {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration();

        final String title = "22 Acacia Avenue";
        final String author = "Maiden @ " + System.currentTimeMillis();
        final String url = "1998 Remastered ";
        final String content = "If you are feeling down, depressed and lonely @" + System.currentTimeMillis();

        AppSyncSubscriptionCall<OnCreatePostSubscription.Data> onCreatePostSubscriptionCall =
            awsAppSyncClient.subscribe(OnCreatePostSubscription.builder().build());
        LatchedSubscriptionCallback<OnCreatePostSubscription.Data> onCreatePostCallback =
            LatchedSubscriptionCallback.instance();
        onCreatePostSubscriptionCall.execute(onCreatePostCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        //Sleep for a while to make sure the subscription goes through
        Sleep.milliseconds(REASONABLE_WAIT_TIME_MS);

        // Try to create post using only the fields that are required for success.
        Posts.addRequiredFieldsOnly(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post using addPostRequireFieldsOnlyMutation ");
        onCreatePostCallback.awaitNextSuccessfulResponse();

        // Try to create a post, by supplying an incomplete set of parameters.
        // Expect errors in the GraphQL response.
        Posts.addMissingRequiredFields(awsAppSyncClient,title, author + System.currentTimeMillis(), url, content);
        Log.d(TAG, "Added Post using addPostMissingRequiredFieldsMutation");
        assertTrue(onCreatePostCallback.awaitNextResponse().hasErrors());

        // Cancel the subscription and expect a completion callback.
        onCreatePostSubscriptionCall.cancel();
        onCreatePostCallback.awaitCompletion();
    }

    @Test
    public void testAddSubscriptionWithIAMAuthModel() {
        testAddSubscriptionWithIAMAuthModel(true);
    }

    @Test
    public void testAddSubscriptionWithIAMAuthModelNoReconnect() {
        testAddSubscriptionWithIAMAuthModel(false);
    }

    private static void testAddSubscriptionWithIAMAuthModel(boolean subscriptionAutoReconnect) {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration(subscriptionAutoReconnect, 0);

        final String title = "Alabama Song [Whisky Bar]";
        final String author = "Doors @ " + System.currentTimeMillis();
        final String url = "The Doors";

        AppSyncSubscriptionCall<OnCreatePostSubscription.Data> onCreatePostSubscriptionCall =
            awsAppSyncClient.subscribe(OnCreatePostSubscription.builder().build());
        LatchedSubscriptionCallback<OnCreatePostSubscription.Data> onCreatePostCallback =
            LatchedSubscriptionCallback.instance();
        onCreatePostSubscriptionCall.execute(onCreatePostCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        // Sleep for a while to make sure the subscription goes through
        Sleep.milliseconds(REASONABLE_WAIT_TIME_MS);

        // Create a post.
        String firstPostContent = "Well, show me the way, to the next whisky bar @" + System.currentTimeMillis();
        Posts.add(awsAppSyncClient, title, author, url, firstPostContent);
        Log.d(TAG, "Added Post");

        // Did it show up on the subscription?
        onCreatePostCallback.awaitNextSuccessfulResponse();

        // Close the subscription and expect a completion callback.
        Log.d(TAG, "Going to cancel subscription");
        onCreatePostSubscriptionCall.cancel();
        onCreatePostCallback.awaitCompletion();

        // Add another post. The expectation is that we will NOT get a message
        // on the subscription, since we just closed it.
        String secondPostContent = "Well, show me the way, to the next whisky bar @" + System.currentTimeMillis();
        Posts.add(awsAppSyncClient, title, author, url, secondPostContent);
        onCreatePostCallback.expectNoResponse();
    }

    /**
     * The mode of operation for network connections hosting a subscription. If the
     * network goes down, what should happen to the subscription?
     */
    enum SubscriptionReconnectMode {
        /**
         * Subscription manager should automatically try to reform the connection,
         * in order to support the subscription.
         */
        AUTOMATICALLY_RECONNECT,

        /**
         * Just let the subscription fail. Don't try to reconnect.
         */
        STAY_DISCONNECTED
    }
}
