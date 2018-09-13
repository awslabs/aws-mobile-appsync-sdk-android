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
import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.DeletePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.GetPostQuery;
import com.amazonaws.mobileconnectors.appsync.demo.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.DeletePostInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.GraphQLCall;
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

    private AWSAppSyncClient mAWSAppSyncClient;

    private static final String TAG = AWSAppSyncQueryInstrumentationTest.class.getSimpleName();

    private Response<GetPostQuery.Data> getPostQueryResponse;
    private Response<AddPostMutation.Data> addPostMutationResponse;
    private Response<DeletePostMutation.Data> deletePostMutationResponse;
    private Response<UpdatePostMutation.Data> updatePostMutationResponse;

    @Before
    public void setUp() {

            mAWSAppSyncClient = createAppSyncClient();
    }

    @After
    public void tearDown() {
        if (mAWSAppSyncClient != null ) {
            mAWSAppSyncClient.mApolloClient.clearNormalizedCache();
        }
    }


    private AWSAppSyncClient createAppSyncClient() {
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


    @Test
    public void testCRUD() {
        assertNotNull(mAWSAppSyncClient);
        final String title = "Home [Scene Six]";
        final String author = "Dream Theater @ " + System.currentTimeMillis();
        final String url = "Metropolis Part 2";
        final String content = "Shine-Lake of fire @" + System.currentTimeMillis();

        //Add a post
        addPost(title,author,url,content);
        assertNotNull(addPostMutationResponse);
        assertNotNull("CreatePost Response should not be null.", addPostMutationResponse.data());
        assertNotNull(addPostMutationResponse);
        assertNotNull(addPostMutationResponse.data());
        assertNotNull(addPostMutationResponse.data().createPost());
        assertNotNull(addPostMutationResponse.data().createPost().id());
        assertEquals(true, content.equals(addPostMutationResponse.data().createPost().content()));
        assertEquals(true, author.equals(addPostMutationResponse.data().createPost().author()));

        String postID = addPostMutationResponse.data().createPost().id();
        Log.d(TAG, "Added Post ID: " + postID);


        //Check that the post has been propagated to the server
        queryPost(AppSyncResponseFetchers.CACHE_FIRST, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertEquals(postID, getPostQueryResponse.data().getPost().id());
        assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));

        //Check that the post has been propagated to the server
        queryPost(AppSyncResponseFetchers.NETWORK_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertEquals(postID, getPostQueryResponse.data().getPost().id());
        assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));


        //Check that the post has been made it to the cache
        queryPost(AppSyncResponseFetchers.CACHE_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertTrue(postID.equals(getPostQueryResponse.data().getPost().id()));
        assertTrue(content.equals(getPostQueryResponse.data().getPost().content()));



        //Update the Post
        final String updatedContent = "New content coming up @" + System.currentTimeMillis();

        updatePost(postID, updatedContent);
        assertNotNull(updatePostMutationResponse);
        assertNotNull(updatePostMutationResponse.data());
        assertNotNull(updatePostMutationResponse.data().updatePost());
        assertNotNull(updatePostMutationResponse.data().updatePost().content());
        assertEquals(true, updatedContent.equals(updatePostMutationResponse.data().updatePost().content()));
        assertEquals(false, content.equals(updatePostMutationResponse.data().updatePost().content()));

        //Check that the information has been propagated to the server
        queryPost(AppSyncResponseFetchers.NETWORK_FIRST, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertNotNull(getPostQueryResponse.data().getPost().id());
        assertEquals(true, postID.equals(getPostQueryResponse.data().getPost().id()));
        assertEquals(false, content.equals(getPostQueryResponse.data().getPost().content()));
        assertEquals(true, updatedContent.equals(getPostQueryResponse.data().getPost().content()));

        //Check that the information has been updated in the local cache
        queryPost(AppSyncResponseFetchers.CACHE_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNotNull(getPostQueryResponse.data().getPost());
        assertNotNull(getPostQueryResponse.data().getPost().id());
        assertEquals(true, postID.equals(getPostQueryResponse.data().getPost().id()));
        assertEquals(false, content.equals(getPostQueryResponse.data().getPost().content()));
        assertEquals(true, updatedContent.equals(getPostQueryResponse.data().getPost().content()));


        //Delete the Post
        deletePost(postID);
        assertNotNull(deletePostMutationResponse);
        assertNotNull(deletePostMutationResponse.data());
        assertNotNull(deletePostMutationResponse.data().deletePost());
        assertNotNull(deletePostMutationResponse.data().deletePost().id());
        assertTrue( postID.equals(deletePostMutationResponse.data().deletePost().id()));

        //Check that it is gone from the server
        queryPost(AppSyncResponseFetchers.NETWORK_ONLY, postID);
        assertNotNull(getPostQueryResponse);
        assertNotNull(getPostQueryResponse.data());
        assertNull(getPostQueryResponse.data().getPost());
    }


    private void queryPosts(final ResponseFetcher responseFetcher) {

        final CountDownLatch queryCountDownLatch = new CountDownLatch(1);
        Log.d(TAG, "Calling Query AllPosts");
        mAWSAppSyncClient.query(AllPostsQuery.builder().build())
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

    private void queryPost( final ResponseFetcher responseFetcher, final String id) {

        final CountDownLatch queryCountDownLatch = new CountDownLatch(1);
        Log.d(TAG, "Calling Query GetPost");
        mAWSAppSyncClient.query(GetPostQuery.builder().id(id).build())
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


    private void addPost( final String title, final String author, final String url, final String content) {
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
                        ""
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


                mAWSAppSyncClient
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

    private void deletePost(final String id) {
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

                mAWSAppSyncClient
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

    private void updatePost( final String postID, final String content) {
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

                mAWSAppSyncClient
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
}
