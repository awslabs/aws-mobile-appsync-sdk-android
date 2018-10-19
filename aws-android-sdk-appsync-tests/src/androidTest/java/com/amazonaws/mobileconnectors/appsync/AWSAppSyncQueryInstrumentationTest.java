/**
 * Copyright 2018-2018 Amazon.com,
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

import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMissingRequiredFieldsMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AddPostRequiredFieldsOnlyMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.DeletePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnCreatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeleteArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnDeletePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdateArticleSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.OnUpdatePostSubscription;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.DeletePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)

public class AWSAppSyncQueryInstrumentationTest {


    private static final String TAG = AWSAppSyncQueryInstrumentationTest.class.getSimpleName();

    private Response<GetPostQuery.Data> getPostQueryResponse;
    private Response<AllPostsQuery.Data> allPostsResponse;

    private Response<AddPostMutation.Data> addPostMutationResponse;
    private Response<AddPostRequiredFieldsOnlyMutation.Data> addPostRequiredFieldsOnlyMutationResponse;
    private Response<AddPostMissingRequiredFieldsMutation.Data> addPostMissingRequiredFieldsResponse;

    private Response<DeletePostMutation.Data> deletePostMutationResponse;
    private Response<UpdatePostMutation.Data> updatePostMutationResponse;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }


    private AWSAppSyncClient createAppSyncClientWithIAM() {
        InputStream configInputStream = null;
        try {

            configInputStream = InstrumentationRegistry.getContext().getResources().getAssets().open("appsync_test_credentials.json");

            final Scanner in = new Scanner(configInputStream);
            final StringBuilder sb = new StringBuilder();
            while (in.hasNextLine()) {
                sb.append(in.nextLine());
            }
            JSONObject config = new JSONObject(sb.toString());
            String endPoint = config.getString("AppSyncEndpoint");
            String appSyncRegion = config.getString("AppSyncRegion");
            Log.d(TAG, "Connecting to " + endPoint + ", region: " + appSyncRegion + ", using IAM");
            String cognitoIdentityPoolID = config.getString("CognitoIdentityPoolId");
            String cognitoRegion = config.getString("CognitoIdentityPoolRegion");

            if (endPoint == null ||appSyncRegion == null || cognitoIdentityPoolID == null || cognitoRegion == null ) {
                Log.e(TAG, "Unable to read AppSyncEndpoint, AppSyncRegion, CognitoIdentityPoolId, CognitoIdentityPoolRegion from config file ");
                return null;
            }

            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(InstrumentationRegistry.getContext(), cognitoIdentityPoolID, Regions.fromName(cognitoRegion));

            return AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getContext())
                    .credentialsProvider(credentialsProvider)
                    .serverUrl(endPoint)
                    .region(Regions.fromName(appSyncRegion))
                    .persistentMutationsCallback(new PersistentMutationsCallback() {
                        @Override
                        public void onResponse(PersistentMutationsResponse response) {
                            Log.d(TAG, response.getMutationClassName());
                        }

                        @Override
                        public void onFailure(PersistentMutationsError error) {
                            Log.e(TAG, error.getMutationClassName());
                            Log.e(TAG, "Error", error.getException());
                        }
                    })
                    .build();

        }
        catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (JSONException je) {
            Log.e(TAG, je.getLocalizedMessage());
            je.printStackTrace();
        }
        finally {
            if (configInputStream != null ) {
                try {
                    configInputStream.close();
                } catch (IOException ce) {
                    Log.e(TAG,ce.getLocalizedMessage());
                    ce.printStackTrace();
                }
            }
        }
        return null;
    }

    private AWSAppSyncClient createAppSyncClientWithAPIKEY() {
        InputStream configInputStream = null;
        try {

            configInputStream = InstrumentationRegistry.getContext().getResources().getAssets().open("appsync_test_credentials.json");

            final Scanner in = new Scanner(configInputStream);
            final StringBuilder sb = new StringBuilder();
            while (in.hasNextLine()) {
                sb.append(in.nextLine());
            }
            JSONObject config = new JSONObject(sb.toString());
            String endPoint = config.getString("AppSyncEndpointAPIKey");
            String appSyncRegion = config.getString("AppSyncRegionAPIKey");
            String apiKey = config.getString("AppSyncAPIKey");

            if (endPoint == null ||apiKey == null  ) {
                Log.e(TAG, "Unable to read AppSyncEndpointAPIKey, AppSyncRegionAPIKey or AppSyncAPIKey from config file ");
                return null;
            }


            APIKeyAuthProvider provider = new BasicAPIKeyAuthProvider(apiKey);

            return AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getContext())
                    .apiKey(provider)
                    .serverUrl(endPoint)
                    .region(Regions.fromName(appSyncRegion))
                    .persistentMutationsCallback(new PersistentMutationsCallback() {
                        @Override
                        public void onResponse(PersistentMutationsResponse response) {
                            Log.d(TAG, response.getMutationClassName());
                        }

                        @Override
                        public void onFailure(PersistentMutationsError error) {
                            Log.e(TAG, error.getMutationClassName());
                            Log.e(TAG, "Error", error.getException());
                        }
                    })
                    .build();

        }
        catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (JSONException je) {
            Log.e(TAG, je.getLocalizedMessage());
            je.printStackTrace();
        }
        finally {
            if (configInputStream != null ) {
                try {
                    configInputStream.close();
                } catch (IOException ce) {
                    Log.e(TAG,ce.getLocalizedMessage());
                    ce.printStackTrace();
                }
            }
        }
        return null;
    }


    @Test
    public void testMultipleSubscriptionsWithIAM() {

        AWSAppSyncClient awsAppSyncClient = createAppSyncClientWithIAM();

        final CountDownLatch addPostMessageReceivedLatch = new CountDownLatch(1);
        final CountDownLatch updatePostMessageReceivedLatch = new CountDownLatch(5);
        final CountDownLatch deletePostMessageReceivedLatch = new CountDownLatch(1);

        assertNotNull(awsAppSyncClient);
        final String title = "Pull Me Under";
        final String author = "Dream Theater @ " + System.currentTimeMillis();
        final String url = "Dream Theater";
        final String content = "Lost in the sky @" + System.currentTimeMillis();

        //Create post callback
        AppSyncSubscriptionCall.Callback onCreatePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnCreatePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<OnCreatePostSubscription.Data> response) {
                Log.d(TAG,"Add Post Response " + response.data().toString());
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

            }
        };

        //Update Post callback
        AppSyncSubscriptionCall.Callback onUpdatePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnUpdatePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnUpdatePostSubscription.Data> response) {
                Log.d(TAG,"Update Post Response " + response.data().toString());
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
            }
        };

        //Delete Post callback
        AppSyncSubscriptionCall.Callback onDeletePostSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnDeletePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnDeletePostSubscription.Data> response) {
                Log.d(TAG,"Delete Post Response " + response.data().toString());
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
            }
        };

        //Create Article callback
        AppSyncSubscriptionCall.Callback onCreateArticleSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnCreateArticleSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnCreateArticleSubscription.Data> response) {
                Log.d(TAG,"Add Article Response " + response.data().toString());
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Add article Error " + e.getLocalizedMessage());
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "Add Article Subscription terminated.");
            }
        };


        //Update Article callback
        AppSyncSubscriptionCall.Callback onUpdateArticleSubscriptionCallback = new AppSyncSubscriptionCall.Callback<OnUpdateArticleSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnUpdateArticleSubscription.Data> response) {
                Log.d(TAG,"Update Article Response " + response.data().toString());
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Update Article Error " + e.getLocalizedMessage());
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "Update Article Subscription terminated.");
            }
        };

        //Delete Article callback
        AppSyncSubscriptionCall.Callback onDeleteArticleCallback = new AppSyncSubscriptionCall.Callback<OnDeleteArticleSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnDeleteArticleSubscription.Data> response) {
                Log.d(TAG,"Delete Article Response " + response.data().toString());
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Delete Article Error " + e.getLocalizedMessage());
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "Delete Article Subscription terminated.");
            }
        };

        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall addSubWatcher = awsAppSyncClient.subscribe(onCreatePostSubscription);
        addSubWatcher.execute(onCreatePostSubscriptionCallback);
        sleep(5*1000);

        OnUpdatePostSubscription onUpdatePostSubscription = OnUpdatePostSubscription.builder().build();
        AppSyncSubscriptionCall updateSubWatcher = awsAppSyncClient.subscribe(onUpdatePostSubscription);
        updateSubWatcher.execute(onUpdatePostSubscriptionCallback);
        sleep(5*1000);


        OnDeletePostSubscription onDeletePostSubscription = OnDeletePostSubscription.builder().build();
        AppSyncSubscriptionCall deleteSubWatcher = awsAppSyncClient.subscribe(onDeletePostSubscription);
        deleteSubWatcher.execute(onDeletePostSubscriptionCallback);
        sleep(5*1000);

        OnCreateArticleSubscription onCreateArticleSubscription = OnCreateArticleSubscription.builder().build();
        AppSyncSubscriptionCall addArticleSubWatcher = awsAppSyncClient.subscribe(onCreateArticleSubscription);
        addArticleSubWatcher.execute(onCreateArticleSubscriptionCallback);
        sleep(5*1000);

        OnUpdateArticleSubscription onUpdateArticleSubscription = OnUpdateArticleSubscription.builder().build();
        AppSyncSubscriptionCall updateArticleSubWatcher = awsAppSyncClient.subscribe(onUpdateArticleSubscription);
        updateArticleSubWatcher.execute(onUpdateArticleSubscriptionCallback);
        sleep(5*1000);

        OnDeleteArticleSubscription onDeleteArticleSubscription = OnDeleteArticleSubscription.builder().build();
        AppSyncSubscriptionCall deleteArticleSubWatcher = awsAppSyncClient.subscribe(onDeleteArticleSubscription);
        deleteArticleSubWatcher.execute(onDeleteArticleCallback);
        sleep(5*1000);

        Log.d(TAG, "Subscribed and setup callback handlers.");

        addPost(awsAppSyncClient,title,author,url,content);
        String postID = addPostMutationResponse.data().createPost().id();
        Log.d(TAG, "Added Post");

        for ( int i = 0; i < 5; i++ ) {
            updatePost(awsAppSyncClient, postID, "Lost in the sky @" + System.currentTimeMillis());
        }

        Log.d(TAG, "Updated post five times");

        deletePost(awsAppSyncClient,postID);
        Log.d(TAG, "Deleted post");


        try {
            assertTrue(addPostMessageReceivedLatch.await(10, TimeUnit.SECONDS));
            assertTrue(updatePostMessageReceivedLatch.await(10, TimeUnit.SECONDS));
            assertTrue(deletePostMessageReceivedLatch.await(10, TimeUnit.SECONDS));

        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        addSubWatcher.cancel();
        updateSubWatcher.cancel();
        deleteSubWatcher.cancel();

        sleep (3*1000);


    }


    public void testAddSubscriptionWithApiKeyAuthModel() {
        AWSAppSyncClient awsAppSyncClient1 = createAppSyncClientWithAPIKEY();
        final CountDownLatch messageReceivedLatch = new CountDownLatch(1);
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

            }
        };

        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall subscriptionWatcher = awsAppSyncClient1.subscribe(onCreatePostSubscription);
        subscriptionWatcher.execute(onCreatePostSubscriptionCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        //Sleep for a while to make sure the subscription goes through
        try {
            Thread.sleep(5 * 1000);
        }
        catch (InterruptedException ie) {
            Log.d(TAG, "Sleep was interrupted");
        }
        addPost(awsAppSyncClient1,title,author,url,content);
        Log.d(TAG, "Added Post");

        try {
            assertTrue(messageReceivedLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    @Test
    public void testAddSubscriptionWithIAMAuthModelForNullPatching() {
        AWSAppSyncClient awsAppSyncClient = createAppSyncClientWithIAM();
        final CountDownLatch message1ReceivedLatch = new CountDownLatch(1);
        final CountDownLatch message2ExceptionReceivedLatch = new CountDownLatch(1);

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

            }
        };

        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall onCreatePostSubscriptionWatcher = awsAppSyncClient.subscribe(onCreatePostSubscription);
        onCreatePostSubscriptionWatcher.execute(onCreatePostSubscriptionCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        //Sleep for a while to make sure the subscription goes through
        try {
            Thread.sleep(5 * 1000);
        }
        catch (InterruptedException ie) {
            Log.d(TAG, "Sleep was interrupted");
        }

        addPostRequiredFieldsOnlyMutation(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post using addPostRequireFieldsOnlyMutation ");

        try {
            assertTrue(message1ReceivedLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }


        addPostMissingRequiredFieldsMutation(awsAppSyncClient,title, author + System.currentTimeMillis(), url, content);
        Log.d(TAG, "Added Post using addPostMissingRequiredFieldsMutation");

        try {
            assertTrue(message2ExceptionReceivedLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    @Test
    public void testAddSubscriptionWithIAMAuthModel() {
        AWSAppSyncClient awsAppSyncClient = createAppSyncClientWithIAM();
        final CountDownLatch message1ReceivedLatch = new CountDownLatch(1);
        final CountDownLatch message2ReceivedLatch = new CountDownLatch(1);

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

            }
        };

        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall onCreatePostSubscriptionWatcher = awsAppSyncClient.subscribe(onCreatePostSubscription);
        onCreatePostSubscriptionWatcher.execute(onCreatePostSubscriptionCallback);
        Log.d(TAG, "Subscribed and setup callback handler.");

        //Sleep for a while to make sure the subscription goes through
        try {
            Thread.sleep(5 * 1000);
        }
        catch (InterruptedException ie) {
           Log.d(TAG, "Sleep was interrupted");
        }
        addPost(awsAppSyncClient,title,author,url,content);
        Log.d(TAG, "Added Post");

        try {
            assertTrue(message1ReceivedLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        //Unsubscribe
        Log.d(TAG, "Going to cancel subscription");
        onCreatePostSubscriptionWatcher.cancel();


        //Add another post. The expectation is that we will not get a message (wait for 10 seconds to be sure)
        addPost(awsAppSyncClient,title,author,url,"Well, show me the way, to the next whisky bar @" + System.currentTimeMillis());
        try {
            assertFalse(message2ReceivedLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    @Test
    public void testSyncOnlyBaseQuery() {
        final CountDownLatch syncLatch = new CountDownLatch(1);
        AWSAppSyncClient awsAppSyncClient = createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);

        Query<AllPostsQuery.Data, AllPostsQuery.Data, com.apollographql.apollo.api.Operation.Variables> baseQuery =  AllPostsQuery.builder().build();
        GraphQLCall.Callback baseQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                allPostsResponse  =  response;
                syncLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {

            }
        };

        awsAppSyncClient.sync(baseQuery, baseQueryCallback, null, null, null, null, 0);

        try {
            syncLatch.await();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        assertNotNull(allPostsResponse);
        assertNotNull(allPostsResponse.data());
        assertNotNull(allPostsResponse.data().listPosts());
        assertNotNull(allPostsResponse.data().listPosts().items());
        assertTrue(allPostsResponse.data().listPosts().items().size() > 0);
        Log.d(TAG, "All Posts " + allPostsResponse.data().listPosts().items().get(0));

    }


    public void testSyncOnlyBaseAndDeltaQuery() {
        final CountDownLatch baseQueryLatch = new CountDownLatch(1);
        final CountDownLatch deltaQueryLatch = new CountDownLatch(1);
        AWSAppSyncClient awsAppSyncClient = createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);

        Query baseQuery =  AllPostsQuery.builder().build();
        GraphQLCall.Callback baseQueryCallback = new GraphQLCall.Callback<AllPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<AllPostsQuery.Data> response) {
                allPostsResponse  =  response;
                baseQueryLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                assertNull(e);
                baseQueryLatch.countDown();
            }
        };

        final Query deltaQuery = GetPostQuery.builder().id("ce228ceb-c2fc-483e-8c3e-3d33fb8dd61f").build();
        GraphQLCall.Callback deltaQueryCallback = new GraphQLCall.Callback<GetPostQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<GetPostQuery.Data> response) {
                getPostQueryResponse = response;
                deltaQueryLatch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                assertNull(e);
                deltaQueryLatch.countDown();
            }
        };

        awsAppSyncClient.sync(baseQuery, baseQueryCallback,null, null, deltaQuery, deltaQueryCallback, 0);

        try {
            baseQueryLatch.await(10, TimeUnit.SECONDS);
            deltaQueryLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        assertNotNull(allPostsResponse);
        assertNotNull(allPostsResponse.data());
        assertNotNull(allPostsResponse.data().listPosts());
        assertNotNull(allPostsResponse.data().listPosts().items());
        assertTrue(allPostsResponse.data().listPosts().items().size() > 0);
        Log.d(TAG, "All Posts " + allPostsResponse.data().listPosts().items().get(0));

    }



    public void testCache() {
        AWSAppSyncClient awsAppSyncClient = createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        String postID = "ce228ceb-c2fc-483e-8c3e-3d33fb8dd61f";

        queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY,postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertEquals(postID, getPostQueryResponse.data().getPost().id());
        getPostQueryResponse = null;

        queryPost(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY,postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertEquals(postID, getPostQueryResponse.data().getPost().id());


    }

    @Test
    public void testCRUD() {
        AWSAppSyncClient awsAppSyncClient = createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        final String title = "Home [Scene Six]";
        final String author = "Dream Theater @ " + System.currentTimeMillis();
        final String url = "Metropolis Part 2";
        final String content = "Shine-Lake of fire @" + System.currentTimeMillis();

        //Add a post
        addPost(awsAppSyncClient,title,author,url,content);
        assertNotNull(addPostMutationResponse);
        assertNotNull("CreatePost Response should not be null.", addPostMutationResponse.data());
        assertNotNull(addPostMutationResponse);
        assertNotNull(addPostMutationResponse.data());
        assertNotNull(addPostMutationResponse.data().createPost());
        assertNotNull(addPostMutationResponse.data().createPost().id());

        String postID = addPostMutationResponse.data().createPost().id();
        Log.d(TAG, "Added Post ID: " + postID);


        //Check that the post has been propagated to the server
        queryPost(awsAppSyncClient, AppSyncResponseFetchers.CACHE_FIRST, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertEquals(postID, getPostQueryResponse.data().getPost().id());
        assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));

        //Check that the post has been propagated to the server
        queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertEquals(postID, getPostQueryResponse.data().getPost().id());
        assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));


        //Check that the post has been made it to the cache
        queryPost(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertTrue(postID.equals(getPostQueryResponse.data().getPost().id()));
        assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));



        //Update the Post
        final String updatedContent = "New content coming up @" + System.currentTimeMillis();

        updatePost(awsAppSyncClient, postID, updatedContent);
        assertNotNull(updatePostMutationResponse);
        assertNotNull(updatePostMutationResponse.data());
        assertNotNull(updatePostMutationResponse.data().updatePost());
        assertNotNull(updatePostMutationResponse.data().updatePost().content());
        assertEquals(true, updatedContent.equals(updatePostMutationResponse.data().updatePost().content()));
        assertEquals(false, content.equals(updatePostMutationResponse.data().updatePost().content()));

        //Check that the information has been propagated to the server
        queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_FIRST, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertNotNull(getPostQueryResponse.data().getPost().id());
        assertEquals(true, postID.equals(getPostQueryResponse.data().getPost().id()));
        assertEquals(false, content.equals(getPostQueryResponse.data().getPost().content()));
        assertEquals(true, updatedContent.equals(getPostQueryResponse.data().getPost().content()));

        //Check that the information has been updated in the local cache
        queryPost(awsAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertNotNull(getPostQueryResponse.data().getPost().id());
        assertEquals(true, postID.equals(getPostQueryResponse.data().getPost().id()));
        assertEquals(false, content.equals(getPostQueryResponse.data().getPost().content()));
        assertEquals(true, updatedContent.equals(getPostQueryResponse.data().getPost().content()));


        //Delete the Post
        deletePost(awsAppSyncClient,postID);
        assertNotNull(deletePostMutationResponse);
        assertNotNull(deletePostMutationResponse.data());
        assertNotNull(deletePostMutationResponse.data().deletePost());
        assertNotNull(deletePostMutationResponse.data().deletePost().id());
        assertTrue( postID.equals(deletePostMutationResponse.data().deletePost().id()));

        //Check that it is gone from the server
        queryPost(awsAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNull(getPostQueryResponse.data().getPost());
    }

    private void queryPosts(AWSAppSyncClient awsAppSyncClient, final ResponseFetcher responseFetcher) {

        final CountDownLatch queryCountDownLatch = new CountDownLatch(1);
        Log.d(TAG, "Calling Query AllPosts");
        awsAppSyncClient.query(AllPostsQuery.builder().build())
                .responseFetcher(responseFetcher)
                .enqueue(new GraphQLCall.Callback<AllPostsQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull final Response<AllPostsQuery.Data> response) {
                        queryCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        e.printStackTrace();
                        //Set to null to indicate failure
                        queryCountDownLatch.countDown();
                    }
                });

        try {
            queryCountDownLatch.await();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    private void queryPost( AWSAppSyncClient awsAppSyncClient, final ResponseFetcher responseFetcher, final String id) {

        final CountDownLatch queryCountDownLatch = new CountDownLatch(1);
        Log.d(TAG, "Calling Query GetPost");
        awsAppSyncClient.query(GetPostQuery.builder().id(id).build())
                .responseFetcher(responseFetcher)
                .enqueue(new GraphQLCall.Callback<GetPostQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<GetPostQuery.Data> response) {
                        getPostQueryResponse = response;
                        queryCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        e.printStackTrace();
                        //Set to null to indicate failure
                        getPostQueryResponse = null;
                        queryCountDownLatch.countDown();
                    }
                });
        try {
            queryCountDownLatch.await();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    private void addPost(final AWSAppSyncClient awsAppSyncClient, final String title, final String author, final String url, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);


        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

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


                awsAppSyncClient
                        .mutate(addPostMutation, expected)
                        .enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                                addPostMutationResponse = response;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                //Set to null to indicate failure
                                addPostMutationResponse = null;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    private void addPostRequiredFieldsOnlyMutation(final AWSAppSyncClient awsAppSyncClient, final String title, final String author, final String url, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);


        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                AddPostRequiredFieldsOnlyMutation.Data expected = new AddPostRequiredFieldsOnlyMutation.Data( null);

                CreatePostInput createPostInput = CreatePostInput.builder()
                        .title(title)
                        .author(author)
                        .url(url)
                        .content(content)
                        .ups(1)
                        .downs(0)
                        .build();

                final AddPostRequiredFieldsOnlyMutation addPostRequiredFieldsOnlyMutation = AddPostRequiredFieldsOnlyMutation.builder().input(createPostInput).build();


                awsAppSyncClient
                        .mutate(addPostRequiredFieldsOnlyMutation, expected)
                        .enqueue(new GraphQLCall.Callback<AddPostRequiredFieldsOnlyMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<AddPostRequiredFieldsOnlyMutation.Data> response) {
                                addPostRequiredFieldsOnlyMutationResponse = response;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                //Set to null to indicate failure
                                addPostRequiredFieldsOnlyMutationResponse = null;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    private void addPostMissingRequiredFieldsMutation(final AWSAppSyncClient awsAppSyncClient, final String title, final String author, final String url, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);


        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                AddPostMissingRequiredFieldsMutation.Data expected = new AddPostMissingRequiredFieldsMutation.Data(null);

                CreatePostInput createPostInput = CreatePostInput.builder()
                        .title(title)
                        .author(author)
                        .url(url)
                        .content(content)
                        .ups(1)
                        .downs(0)
                        .build();

                final AddPostMissingRequiredFieldsMutation missingRequiredFieldsMutation = AddPostMissingRequiredFieldsMutation.builder().input(createPostInput).build();


                awsAppSyncClient
                        .mutate(missingRequiredFieldsMutation, expected)
                        .enqueue(new GraphQLCall.Callback<AddPostMissingRequiredFieldsMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<AddPostMissingRequiredFieldsMutation.Data> response) {
                                addPostMissingRequiredFieldsResponse = response;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                //Set to null to indicate failure
                                addPostRequiredFieldsOnlyMutationResponse = null;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    private void deletePost(final AWSAppSyncClient awsAppSyncClient, final String id) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                DeletePostMutation.Data expected = new DeletePostMutation.Data(new DeletePostMutation.DeletePost(
                        "Post",
                        "",
                        "",
                        "",
                        "",
                        ""
                ));

                DeletePostInput deletePostInput = DeletePostInput.builder()
                        .id(id)
                        .build();

                DeletePostMutation deletePostMutation = DeletePostMutation.builder().input(deletePostInput).build();

                Log.d(TAG, "Calling Delete");

                awsAppSyncClient
                        .mutate(deletePostMutation, expected)
                        .enqueue(new GraphQLCall.Callback<DeletePostMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<DeletePostMutation.Data> response) {
                                deletePostMutationResponse = response;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                //Set to null to indicate failure
                                deletePostMutationResponse = null;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    private void updatePost( final AWSAppSyncClient awsAppSyncClient, final String postID, final String content) {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                UpdatePostMutation.Data expected = new UpdatePostMutation.Data( new UpdatePostMutation.UpdatePost(
                        "Post",
                        "",
                        "",
                        "",
                        "",
                        "",
                         0
                ));

                UpdatePostInput updatePostInput = UpdatePostInput.builder()
                        .id(postID)
                        .content(content)
                        .build();

                UpdatePostMutation updatePostMutation = UpdatePostMutation.builder().input(updatePostInput).build();

                Log.d(TAG, "Calling Update");

                awsAppSyncClient
                        .mutate(updatePostMutation, expected)
                        .enqueue(new GraphQLCall.Callback<UpdatePostMutation.Data>() {
                            @Override
                            public void onResponse(@Nonnull final Response<UpdatePostMutation.Data> response) {
                                updatePostMutationResponse = response;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }

                            @Override
                            public void onFailure(@Nonnull final ApolloException e) {
                                e.printStackTrace();
                                //Set to null to indicate failure
                                updatePostMutationResponse = null;
                                mCountDownLatch.countDown();
                                if (Looper.myLooper() != null) {
                                    Looper.myLooper().quit();
                                }
                            }
                        });
                Looper.loop();

            }
        }).start();

        Log.d(TAG, "Waiting for latch to be counted down");
        try {
            assertTrue(mCountDownLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    private void sleep(int time) {
        //Sleep for a while to make sure the cancel goes through
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException ie) {
            Log.d(TAG, "Sleep was interrupted");
        }

    }
}
