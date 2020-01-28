/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.CommentOnEventMutation;
import com.amazonaws.mobileconnectors.appsync.demo.NewCommentOnEventSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeleteArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeletePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.apollographql.apollo.api.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class AWSAppSyncSubscriptionInstrumentationTest {
    private static final String TAG = AWSAppSyncSubscriptionInstrumentationTest.class.getSimpleName();
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);
    private static AppSyncTestSetupHelper appSyncTestSetupHelper;

    @BeforeClass
    public static void setupOnce() {
        appSyncTestSetupHelper = new AppSyncTestSetupHelper();
    }

    @Before
    @After
    public void enableWifiIfNotEnabled() {
        Context context = InstrumentationRegistry.getContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (WifiManager.WIFI_STATE_ENABLED != wifiManager.getWifiState()) {
            wifiManager.setWifiEnabled(true);
            Sleep.milliseconds(REASONABLE_WAIT_TIME_MS);
        }
        assertEquals("Wifi is not enabled.", WifiManager.WIFI_STATE_ENABLED, wifiManager.getWifiState());
    }

    /**
     * Validate that subscriptions work with IAM credentials. To test, form a subscription,
     * create a model, and then validate the subscription sees the created thing.
     */
    @Test
    public void testSubscriptionWithApiKey() {
        // Get a client handle
        AWSAppSyncClient awsAppSyncClient =
            appSyncTestSetupHelper.createAppSyncClientWithApiKeyForGogiTest();

        // Create a subscription that listens for new comments that are made on events.
        AppSyncSubscriptionCall<NewCommentOnEventSubscription.Data> onNextCommentSubscriptionCall =
            awsAppSyncClient.subscribe(NewCommentOnEventSubscription.builder()
                .eventId("9a95ab20-cd3e-43ea-a04e-93769a531a00")
                .build());
        LatchedSubscriptionCallback<NewCommentOnEventSubscription.Data> subscriptionCallback =
            LatchedSubscriptionCallback.instance();
        onNextCommentSubscriptionCall.execute(subscriptionCallback);

        // Wait for the subscription to setup
        Sleep.milliseconds(REASONABLE_WAIT_TIME_MS);

        // Create a comment, associated to an event ID.
        awsAppSyncClient.mutate(CommentOnEventMutation.builder()
            .eventId("9a95ab20-cd3e-43ea-a04e-93769a531a00")
            .content("Comments from test" + System.currentTimeMillis())
            .createdAt("fromTest")
            .build()
        ).enqueue(NoOpGraphQLCallback.<CommentOnEventMutation.Data>instance());

        // Okay. Did we see it on the subscription?
        subscriptionCallback.awaitNextSuccessfulResponse();

        // Validate that the subscription is completed when cancel() is called on it.
        onNextCommentSubscriptionCall.cancel();
        subscriptionCallback.awaitCompletion();
    }

    /**
     * Create a *bunch* of subscriptions using IAM auth, and auto-reconnect retry mode.
     * Make sure that all of the stuff we mutate shows up over the various subscriptions.
     */
    @Test
    public void testMultipleSubscriptionsWithIAM() {
        testMultipleSubscriptionsWithIAM(SubscriptionReconnectMode.AUTOMATICALLY_RECONNECT);
    }

    /**
     * Create a *bunch* of subscriptions using IAM auth, and no-reconnect retry mode.
     * Make sure that all of the stuff we mutate shows up over the various subscriptions.
     */
    @Test
    public void testMultipleSubscriptionsWithIAMNoReconnect() {
        testMultipleSubscriptionsWithIAM(SubscriptionReconnectMode.STAY_DISCONNECTED);
    }

    private void testMultipleSubscriptionsWithIAM(SubscriptionReconnectMode subscriptionReconnectMode) {
        final boolean shouldAutomaticallyReconnect =
            SubscriptionReconnectMode.AUTOMATICALLY_RECONNECT.equals(subscriptionReconnectMode);
        AWSAppSyncClient awsAppSyncClient =
            appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration(shouldAutomaticallyReconnect, 0);

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
                appSyncTestSetupHelper.addPost(awsAppSyncClient, title, author, url, content);
            assertNotNull(addPostMutationResponse.data());

            AddPostMutation.CreatePost createPost = addPostMutationResponse.data().createPost();
            assertNotNull(createPost);

            String postId = createPost.id();
            Log.d(TAG, "Added Post with ID = " + postId);

            // Update that same post, 5 times.
            for (int i = 0; i < 5; i++) {
                Response<UpdatePostMutation.Data> updatePostMutationResponse = appSyncTestSetupHelper.updatePost(
                    awsAppSyncClient,
                    postId,
                    "Lost in the sky @" + System.currentTimeMillis());
                assertNotNull(updatePostMutationResponse);
            }
            Log.d(TAG, "Updated post five times");

            // Okay, now delete the post.
            appSyncTestSetupHelper.deletePost(awsAppSyncClient, postId);
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

    private void testAddSubscriptionWithApiKeyAuthModel(SubscriptionReconnectMode subscriptionReconnectMode) {
        boolean shouldAutomaticallyReconnect =
            SubscriptionReconnectMode.AUTOMATICALLY_RECONNECT.equals(subscriptionReconnectMode);
        AWSAppSyncClient awsAppSyncClient =
            appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration(shouldAutomaticallyReconnect, 0);

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

        appSyncTestSetupHelper.addPost(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post");

        // Did the post show up on the subscription?
        onCreatePostCallback.awaitNextSuccessfulResponse();

        // Cancel the subscription call, and expect a completion callback.
        onCreatePostSubscriptionCall.cancel();
        onCreatePostCallback.awaitCompletion();
    }

    @Test
    public void testAddSubscriptionWithIAMAuthModelForNullPatching() {
        AWSAppSyncClient awsAppSyncClient =
            appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();

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
        appSyncTestSetupHelper.addPostRequiredFieldsOnlyMutation(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post using addPostRequireFieldsOnlyMutation ");
        onCreatePostCallback.awaitNextSuccessfulResponse();

        // Try to create a post, by supplying an incomplete set of parameters.
        // Expect errors in the GraphQL response.
        appSyncTestSetupHelper.addPostMissingRequiredFieldsMutation(awsAppSyncClient,title, author + System.currentTimeMillis(), url, content);
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

    private void testAddSubscriptionWithIAMAuthModel(boolean subscriptionAutoReconnect) {
        AWSAppSyncClient awsAppSyncClient =
            appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration(subscriptionAutoReconnect, 0);

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
        appSyncTestSetupHelper.addPost(awsAppSyncClient, title, author, url, firstPostContent);
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
        appSyncTestSetupHelper.addPost(awsAppSyncClient, title, author, url, secondPostContent);
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
