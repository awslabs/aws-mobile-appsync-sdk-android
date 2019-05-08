/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 * <p>
 * http://aws.amazon.com/asl/
 * <p>
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.Suppress;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeleteArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeletePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class AWSAppSyncQueryInstrumentationTest {
    private static final String TAG = AWSAppSyncQueryInstrumentationTest.class.getSimpleName();

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
            try {
                Thread.sleep(3 * 1000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        assert(wifiManager.setWifiEnabled(true));
    }

    @Test
    public void testMultipleSubscriptionsWithIAM() {
        testMultipleSubscriptionsWithIAM(true);
    }

    @Test
    @Suppress
    public void testMultipleSubscriptionsWithIAMNoReconnect() {
        testMultipleSubscriptionsWithIAM(false);
    }

    private void testMultipleSubscriptionsWithIAM(boolean subscriptionsAutoReconnect) {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration(subscriptionsAutoReconnect, 0);
        assertNotNull(awsAppSyncClient);

        for (int iteration = 0 ; iteration < 3; iteration ++ ) {

            final CountDownLatch addPostMessageReceivedLatch = new CountDownLatch(1);
            final CountDownLatch updatePostMessageReceivedLatch = new CountDownLatch(5);
            final CountDownLatch deletePostMessageReceivedLatch = new CountDownLatch(1);

            final CountDownLatch onCreatePostSubscriptionCompletionLatch = new CountDownLatch(1);
            final CountDownLatch onUpdatePostSubscriptionCompletionLatch = new CountDownLatch(1);
            final CountDownLatch onDeletePostSubscriptionCompletionLatch = new CountDownLatch(1);
            final CountDownLatch onCreateArticleSubscriptionCompletionLatch = new CountDownLatch(1);
            final CountDownLatch onUpdateArticleSubscriptionCompletionLatch = new CountDownLatch(1);
            final CountDownLatch onDeleteArticleSubscriptionCompletionLatch = new CountDownLatch(1);

            final String title = "Pull Me Under";
            final String author = "Dream Theater @ " + System.currentTimeMillis();
            final String url = "Dream Theater";
            final String content = "Lost in the sky @" + System.currentTimeMillis();

            //Create post callback
            AppSyncSubscriptionCall.Callback onCreatePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnCreatePostSubscription.Data>() {
                @Override
                public void onResponse(@Nonnull final Response<OnCreatePostSubscription.Data> response) {
                    assertNotNull(response);
                    assertNotNull(response.data());
                    Log.d(TAG, "Add Post Response " + response.data().toString());
                    addPostMessageReceivedLatch.countDown();
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "Add Post Error " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "Add Post Subscription terminated.");
                    onCreatePostSubscriptionCompletionLatch.countDown();
                }
            };

            //Update Post callback
            AppSyncSubscriptionCall.Callback onUpdatePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnUpdatePostSubscription.Data>() {
                @Override
                public void onResponse(@Nonnull Response<OnUpdatePostSubscription.Data> response) {
                    assertNotNull(response);
                    assertNotNull(response.data());
                    Log.d(TAG, "Update Post Response " + response.data().toString());
                    updatePostMessageReceivedLatch.countDown();
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "Update Post Error " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "Update Post Subscription terminated.");
                    onUpdatePostSubscriptionCompletionLatch.countDown();
                }
            };

            //Delete Post callback
            AppSyncSubscriptionCall.Callback onDeletePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnDeletePostSubscription.Data>() {
                @Override
                public void onResponse(@Nonnull Response<OnDeletePostSubscription.Data> response) {
                    assertNotNull(response);
                    assertNotNull(response.data());
                    Log.d(TAG, "Delete Post Response " + response.data().toString());
                    deletePostMessageReceivedLatch.countDown();
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "Delete Post Error " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "Delete Post Subscription terminated.");
                    onDeletePostSubscriptionCompletionLatch.countDown();
                }
            };

            //Create Article callback
            AppSyncSubscriptionCall.Callback onCreateArticleSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnCreateArticleSubscription.Data>() {
                @Override
                public void onResponse(@Nonnull Response<OnCreateArticleSubscription.Data> response) {
                    assertNotNull(response);
                    assertNotNull(response.data());
                    Log.d(TAG, "Add Article Response " + response.data().toString());
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "Add article Error " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "Add Article Subscription terminated.");
                    onCreateArticleSubscriptionCompletionLatch.countDown();
                }
            };


            //Update Article callback
            AppSyncSubscriptionCall.Callback onUpdateArticleSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnUpdateArticleSubscription.Data>() {
                @Override
                public void onResponse(@Nonnull Response<OnUpdateArticleSubscription.Data> response) {
                    assertNotNull(response);
                    assertNotNull(response.data());
                    Log.d(TAG, "Update Article Response " + response.data().toString());
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "Update Article Error " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "Update Article Subscription terminated.");
                    onUpdateArticleSubscriptionCompletionLatch.countDown();
                }
            };

            //Delete Article callback
            AppSyncSubscriptionCall.Callback onDeleteArticleCallback = new AppSyncSubscriptionCall.Callback<OnDeleteArticleSubscription.Data>() {
                @Override
                public void onResponse(@Nonnull Response<OnDeleteArticleSubscription.Data> response) {
                    assertNotNull(response);
                    assertNotNull(response.data());
                    Log.d(TAG, "Delete Article Response " + response.data().toString());
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    Log.e(TAG, "Delete Article Error " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "Delete Article Subscription terminated.");
                    onDeleteArticleSubscriptionCompletionLatch.countDown();
                }
            };

            OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
            AppSyncSubscriptionCall addSubWatcher = awsAppSyncClient.subscribe(onCreatePostSubscription);
            addSubWatcher.execute(onCreatePostSubscriptionCallback);

            OnUpdatePostSubscription onUpdatePostSubscription = OnUpdatePostSubscription.builder().build();
            AppSyncSubscriptionCall updateSubWatcher = awsAppSyncClient.subscribe(onUpdatePostSubscription);
            updateSubWatcher.execute(onUpdatePostSubscriptionCallback);


            OnDeletePostSubscription onDeletePostSubscription = OnDeletePostSubscription.builder().build();
            AppSyncSubscriptionCall deleteSubWatcher = awsAppSyncClient.subscribe(onDeletePostSubscription);
            deleteSubWatcher.execute(onDeletePostSubscriptionCallback);

            OnCreateArticleSubscription onCreateArticleSubscription = OnCreateArticleSubscription.builder().build();
            AppSyncSubscriptionCall addArticleSubWatcher = awsAppSyncClient.subscribe(onCreateArticleSubscription);
            addArticleSubWatcher.execute(onCreateArticleSubscriptionCallback);

            OnUpdateArticleSubscription onUpdateArticleSubscription = OnUpdateArticleSubscription.builder().build();
            AppSyncSubscriptionCall updateArticleSubWatcher = awsAppSyncClient.subscribe(onUpdateArticleSubscription);
            updateArticleSubWatcher.execute(onUpdateArticleSubscriptionCallback);


            OnDeleteArticleSubscription onDeleteArticleSubscription = OnDeleteArticleSubscription.builder().build();
            AppSyncSubscriptionCall deleteArticleSubWatcher = awsAppSyncClient.subscribe(onDeleteArticleSubscription);
            deleteArticleSubWatcher.execute(onDeleteArticleCallback);

            appSyncTestSetupHelper.sleep(60 * 1000);
            Log.d(TAG, "Subscribed and setup callback handlers.");

            Response<AddPostMutation.Data> addPostMutationResponse = appSyncTestSetupHelper.addPost(awsAppSyncClient, title, author, url, content);
            String postID = addPostMutationResponse.data().createPost().id();
            Log.d(TAG, "Added Post");

            for (int i = 0; i < 5; i++) {
                Response<UpdatePostMutation.Data> updatePostMutationResponse = appSyncTestSetupHelper.updatePost(
                        awsAppSyncClient,
                        postID,
                        "Lost in the sky @" + System.currentTimeMillis());
                assertNotNull(updatePostMutationResponse);
            }

            Log.d(TAG, "Updated post five times");

            appSyncTestSetupHelper.deletePost(awsAppSyncClient, postID);
            Log.d(TAG, "Deleted post");

            try {
                assertTrue(addPostMessageReceivedLatch.await(60, TimeUnit.SECONDS));
                assertTrue(updatePostMessageReceivedLatch.await(60, TimeUnit.SECONDS));
                assertTrue(deletePostMessageReceivedLatch.await(60, TimeUnit.SECONDS));

            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }

            addSubWatcher.cancel();
            updateSubWatcher.cancel();
            deleteSubWatcher.cancel();
            addArticleSubWatcher.cancel();
            updateArticleSubWatcher.cancel();
            deleteArticleSubWatcher.cancel();

            try {
                assertTrue(onCreatePostSubscriptionCompletionLatch.await(60, TimeUnit.SECONDS));
                assertTrue(onUpdatePostSubscriptionCompletionLatch.await(60, TimeUnit.SECONDS));
                assertTrue(onDeletePostSubscriptionCompletionLatch.await(60, TimeUnit.SECONDS));
                assertTrue(onCreateArticleSubscriptionCompletionLatch.await(60, TimeUnit.SECONDS));
                assertTrue(onUpdateArticleSubscriptionCompletionLatch.await(60, TimeUnit.SECONDS));
                assertTrue(onDeleteArticleSubscriptionCompletionLatch.await(60, TimeUnit.SECONDS));

            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }
        }
    }

    @Test
    public void testAddSubscriptionWithApiKeyAuthModel() {
        testAddSubscriptionWithApiKeyAuthModel(true);
    }

    @Test
    @Suppress
    public void testAddSubscriptionWithApiKeyAuthModelNoReconnect() {
        testAddSubscriptionWithApiKeyAuthModel(false);
    }

    private void testAddSubscriptionWithApiKeyAuthModel(boolean subscriptionsAutoRecconect) {
        AWSAppSyncClient awsAppSyncClient1 = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration(subscriptionsAutoRecconect, 0);
        final CountDownLatch messageReceivedLatch = new CountDownLatch(1);
        final CountDownLatch subscriptionCompletedLatch = new CountDownLatch(1);
        assertNotNull(awsAppSyncClient1);
        final String title = "Alabama Song [Whisky Bar]";
        final String author = "Doors @ " + System.currentTimeMillis();
        final String url = "The Doors";
        final String content = "Well, show me the way, to the next whisky bar @" + System.currentTimeMillis();

        AppSyncSubscriptionCall.Callback onCreatePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnCreatePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<OnCreatePostSubscription.Data> response) {
                Log.d(TAG,"Response " + response.data().toString());
                messageReceivedLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Error " + e.getLocalizedMessage());
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "Subscription Completed");
                subscriptionCompletedLatch.countDown();
            }
        };

        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall subscriptionWatcher = awsAppSyncClient1.subscribe(onCreatePostSubscription);
        subscriptionWatcher.execute(onCreatePostSubscriptionCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        //Sleep for a while to make sure the subscription goes through
        try {
            Thread.sleep(10 * 1000);
        }
        catch (InterruptedException ie) {
            Log.d(TAG, "Sleep was interrupted");
        }

        appSyncTestSetupHelper.addPost(awsAppSyncClient1,title,author,url,content);
        Log.d(TAG, "Added Post");

        try {
            assertTrue(messageReceivedLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        subscriptionWatcher.cancel();
        try {
            assertTrue(subscriptionCompletedLatch.await(60, TimeUnit.SECONDS));
        }
        catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    @Test
    public void testAddSubscriptionWithIAMAuthModelForNullPatching() {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
        final CountDownLatch message1ReceivedLatch = new CountDownLatch(1);
        final CountDownLatch message2ExceptionReceivedLatch = new CountDownLatch(1);
        final CountDownLatch subscriptionCompletedLatch = new CountDownLatch(1);

        assertNotNull(awsAppSyncClient);
        assertNotNull(awsAppSyncClient);
        final String title = "22 Acacia Avenue";
        final String author = "Maiden @ " + System.currentTimeMillis();
        final String url = "1998 Remastered ";
        final String content = "If you are feeling down, depressed and lonely @" + System.currentTimeMillis();

        AppSyncSubscriptionCall.Callback<OnCreatePostSubscription.Data> onCreatePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnCreatePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<OnCreatePostSubscription.Data> response) {
                Log.d(TAG,"Response " + response.data().toString());
                message1ReceivedLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Error " + e.getLocalizedMessage());
                assertEquals("Failed to parse http response", e.getLocalizedMessage());
                message2ExceptionReceivedLatch.countDown();
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "Subscription Completed");
                subscriptionCompletedLatch.countDown();

            }
        };

        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall onCreatePostSubscriptionWatcher = awsAppSyncClient.subscribe(onCreatePostSubscription);
        onCreatePostSubscriptionWatcher.execute(onCreatePostSubscriptionCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        //Sleep for a while to make sure the subscription goes through
        appSyncTestSetupHelper.sleep(10 * 1000);

        appSyncTestSetupHelper.addPostRequiredFieldsOnlyMutation(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post using addPostRequireFieldsOnlyMutation ");

        try {
            assertTrue(message1ReceivedLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        appSyncTestSetupHelper.addPostMissingRequiredFieldsMutation(awsAppSyncClient,title, author + System.currentTimeMillis(), url, content);
        Log.d(TAG, "Added Post using addPostMissingRequiredFieldsMutation");

        try {
            assertTrue(message2ExceptionReceivedLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        onCreatePostSubscriptionWatcher.cancel();

        try {
            assertTrue(subscriptionCompletedLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    @Test
    public void testAddSubscriptionWithIAMAuthModel() {
        testAddSubscriptionWithIAMAuthModel(true);
    }

    @Test
    @Suppress
    public void testAddSubscriptionWithIAMAuthModelNoReconnect() {
        testAddSubscriptionWithIAMAuthModel(false);
    }


    private void testAddSubscriptionWithIAMAuthModel(boolean subscriptionAutoReconnect) {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration(subscriptionAutoReconnect, 0);
        final CountDownLatch message1ReceivedLatch = new CountDownLatch(1);
        final CountDownLatch message2ReceivedLatch = new CountDownLatch(1);
        final CountDownLatch subscriptionCompletedLatch = new CountDownLatch(1);

        assertNotNull(awsAppSyncClient);
        final String title = "Alabama Song [Whisky Bar]";
        final String author = "Doors @ " + System.currentTimeMillis();
        final String url = "The Doors";
        final String content = "Well, show me the way, to the next whisky bar @" + System.currentTimeMillis();

        AppSyncSubscriptionCall.Callback onCreatePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnCreatePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<OnCreatePostSubscription.Data> response) {
                Log.d(TAG,"Response " + response.data().toString());
                message1ReceivedLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Error " + e.getLocalizedMessage());
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "Received onCompleted on subscription");
                subscriptionCompletedLatch.countDown();

            }
        };

        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall onCreatePostSubscriptionWatcher = awsAppSyncClient.subscribe(onCreatePostSubscription);
        onCreatePostSubscriptionWatcher.execute(onCreatePostSubscriptionCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        //Sleep for a while to make sure the subscription goes through
        try {
            Thread.sleep(10 * 1000);
        }
        catch (InterruptedException ie) {
            Log.d(TAG, "Sleep was interrupted");
        }

        appSyncTestSetupHelper.addPost(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post");

        try {
            assertTrue(message1ReceivedLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        //Unsubscribe
        Log.d(TAG, "Going to cancel subscription");
        onCreatePostSubscriptionWatcher.cancel();

        try {
            assertTrue(subscriptionCompletedLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        //Add another post. The expectation is that we will not get a message (wait for 60 seconds to be sure)
        appSyncTestSetupHelper.addPost(awsAppSyncClient,title,author,url,"Well, show me the way, to the next whisky bar @" + System.currentTimeMillis());
        try {
            assertFalse(message2ReceivedLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    @Test
    public void testSyncOnlyBaseQuery() {
        final CountDownLatch syncLatch = new CountDownLatch(1);
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
        assertNotNull(awsAppSyncClient);

        boolean success = false;
        Query<AllPostsQuery.Data, AllPostsQuery.Data, com.apollographql.apollo.api.Operation.Variables> baseQuery =  AllPostsQuery.builder().build();
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

        Cancelable handle = awsAppSyncClient.sync(baseQuery, baseQueryCallback,0);
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

    @Test
    public void testSyncOnlyBaseAndDeltaQuery() {
        final CountDownLatch baseQueryLatch = new CountDownLatch(1);
        final CountDownLatch deltaQueryLatch = new CountDownLatch(1);
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
        assertNotNull(awsAppSyncClient);

        Query baseQuery =  AllPostsQuery.builder().build();
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

    @Test
    public void testQueryPostsWithUserPoolsAuthorization() {
        AWSAppSyncClient userPoolsAppSyncClientForPosts = appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration();
        Log.d(TAG, "AWSAppSyncClient for AMAZON_COGNITO_USER_POOLS: " + userPoolsAppSyncClientForPosts);

        // Query Posts through API Key Client
        Response<AllPostsQuery.Data> allPostsResponse =
                appSyncTestSetupHelper.listPosts(userPoolsAppSyncClientForPosts,
                        AppSyncResponseFetchers.NETWORK_ONLY)
                        .get("NETWORK");
        assertNotNull(allPostsResponse);
        assertNotNull(allPostsResponse.data());
        AllPostsQuery.ListPosts listPosts = allPostsResponse.data().listPosts();
        assertNotNull(listPosts);

        String postID;
        if (listPosts.items() != null) {
            for (AllPostsQuery.Item item : listPosts.items()) {
                postID = item.id();
                Map<String, Response<GetPostQuery.Data>> responses = appSyncTestSetupHelper.queryPost(
                        userPoolsAppSyncClientForPosts,
                        AppSyncResponseFetchers.CACHE_AND_NETWORK,
                        postID);
                appSyncTestSetupHelper.assertQueryPostResponse(responses.get("CACHE"), postID);
                appSyncTestSetupHelper.assertQueryPostResponse(responses.get("NETWORK"), postID);
            }
        }
    }

    @Test
    public void testCRUDWithSingleClient() {
        List<AWSAppSyncClient> clients = new ArrayList<>();
        clients.add(appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration(false, 2*1000));
        appSyncTestSetupHelper.testCRUD(clients);
    }

    /*
    This test should be run on a physical device or simulator with cellular data turned off.
    The test disables the wifi on the device to create the offline scenario.
     */
    @Test
    public void testMultipleOfflineMutations() {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithUserPoolsFromAWSConfiguration();
        assertNotNull(awsAppSyncClient);

        final String title = "AWSAppSyncMultiClientInstrumentationTest => testMultipleOfflineMutations => Learning to Live ";
        final String author = "Dream Theater @ ";
        final String url = "Dream Theater Station";
        final String content = "No energy for anger @" + System.currentTimeMillis();

        final int numberOfLatches = 25;
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

        Response<GetPostQuery.Data> getPostQueryResponse =
                appSyncTestSetupHelper.queryPost(
                        awsAppSyncClient,
                        AppSyncResponseFetchers.NETWORK_ONLY, postID)
                        .get("NETWORK");
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertNotNull(getPostQueryResponse.data().getPost().author());
        assertEquals(author + (numberOfLatches - 1), getPostQueryResponse.data().getPost().author());
    }

    /*
     This test should be run on a physical device or simulator with cellular data turned off.
     The test disables the wifi on the device to create the offline scenario.
      */
    @Test
    public void testSingleOfflineMutation() {
        final AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
        assertNotNull(awsAppSyncClient);

        final String title = "AWSAppSyncQueryInstrumentationTest => testSingleOfflineMutation => Learning to Live ";
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

        final List<Response<UpdatePostMutation.Data>> updatePostMutationResponses = new ArrayList<>();
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

        Response<GetPostQuery.Data> getPostQueryResponse =
                appSyncTestSetupHelper.queryPost(awsAppSyncClient,
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
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
        assertNotNull(awsAppSyncClient);

        //Try to update a Post with a Fake ID
        final String updatedContent = "New content coming up @" + System.currentTimeMillis();
        final String randomID = UUID.randomUUID().toString();
        Response<UpdatePostMutation.Data> updatePostMutationResponse = appSyncTestSetupHelper.updatePost(awsAppSyncClient, randomID, updatedContent);
        assertNotNull(updatePostMutationResponse);
        assertNull(updatePostMutationResponse.data().updatePost());
        assertNotNull(updatePostMutationResponse.errors());
        Error error = updatePostMutationResponse.errors().get(0);
        assertNotNull(error);
        assertNotNull(error.message());
        assertTrue(error.message().contains("Service: AmazonDynamoDBv2; Status Code: 400; Error Code: ConditionalCheckFailedException;"));

    }

    @Test
    public  void testCancelMutationWithinCallback() {
        AWSAppSyncClient awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
        assertNotNull(awsAppSyncClient);
        final CountDownLatch add2CountDownlatch = new CountDownLatch(1);

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
                .ups(new Integer(1))
                .downs(new Integer(0))
                .build();

        AddPostMutation addPostMutation1 = AddPostMutation.builder().input(createPostInput1).build();
        final AppSyncMutationCall call1 = awsAppSyncClient.mutate(addPostMutation1, expected);

        CreatePostInput createPostInput2 = CreatePostInput.builder()
                .title("Break On Through")
                .author("Doors" + System.currentTimeMillis())
                .url("Doors.com")
                .content("To the other side")
                .ups(new Integer(1))
                .downs(new Integer(0))
                .build();
        AddPostMutation addPostMutation2 = AddPostMutation.builder().input(createPostInput2).build();
        final AppSyncMutationCall call2 = awsAppSyncClient.mutate(addPostMutation2, expected);


        call1.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                call1.cancel();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                e.printStackTrace();
                assertTrue("OnError received for first mutation. Unexpected", false);
            }
        });

        call2.enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                call2.cancel();
                add2CountDownlatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                e.printStackTrace();
                assertTrue("OnError received for Second mutation. Unexpected", false);
                add2CountDownlatch.countDown();
            }
        });

        try {
            add2CountDownlatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException iex) {
            iex.printStackTrace();
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
}
