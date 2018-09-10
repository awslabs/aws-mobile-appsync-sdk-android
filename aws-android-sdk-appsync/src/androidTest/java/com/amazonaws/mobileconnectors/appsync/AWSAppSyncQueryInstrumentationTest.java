package com.amazonaws.mobileconnectors.appsync;

import android.support.test.InstrumentationRegistry;

import com.amazonaws.mobileconnectors.appsync.demo.AddPostMutation;
import com.amazonaws.mobileconnectors.appsync.demo.AllPostsQuery;
import com.amazonaws.mobileconnectors.appsync.demo.ClientFactory;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@Ignore
public class AWSAppSyncQueryInstrumentationTest {

    private AWSAppSyncClient mAWSAppSyncClient;

    private static final String TAG = AWSAppSyncQueryInstrumentationTest.class.getSimpleName();

    Response<AllPostsQuery.Data> queryResponse;

    @Before
    public void setUp() {
        mAWSAppSyncClient = ClientFactory.getInstance(InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() {
        queryResponse = null;
        mAWSAppSyncClient.mApolloClient.clearNormalizedCache();
    }

    @Test
    public void testQuery_NetworkOnly() {
        assertNotNull("AWSAppSyncClient should not be null.", mAWSAppSyncClient);
        queryPosts(AppSyncResponseFetchers.NETWORK_ONLY);
        Log.d(TAG, queryResponse.toString());

        assertNotNull("Items should not be null", queryResponse.data().listPosts().items());
        assertTrue(queryResponse.data().listPosts().items().size() >= 0);
    }

    @Test
    public void testQuery_CacheAndNetwork() {
        assertNotNull("AWSAppSyncClient should not be null.", mAWSAppSyncClient);
        queryPosts(AppSyncResponseFetchers.CACHE_AND_NETWORK);
        Log.d(TAG, queryResponse.toString());
        assertNotNull("Items should not be null", queryResponse.data().listPosts().items());
        assertTrue(queryResponse.data().listPosts().items().size() >= 0);
    }

    @Test
    public void testQuery_CacheFirst() {
        assertNotNull("AWSAppSyncClient should not be null.", mAWSAppSyncClient);
        queryPosts(AppSyncResponseFetchers.CACHE_FIRST);
        Log.d(TAG, queryResponse.toString());
        assertNotNull("Items should not be null", queryResponse.data().listPosts().items());
        assertTrue(queryResponse.data().listPosts().items().size() >= 0);
    }

    @Test
    public void testQuery_NetworkFirst() {
        assertNotNull("AWSAppSyncClient should not be null.", mAWSAppSyncClient);
        queryPosts(AppSyncResponseFetchers.NETWORK_FIRST);
        Log.d(TAG, queryResponse.toString());
        assertNotNull("Items should not be null", queryResponse.data().listPosts().items());
        assertTrue(queryResponse.data().listPosts().items().size() >= 0);
    }

    @Test
    public void testAddMutation() {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        final String title = "Keto diet is gaining traction";
        final String author = "Dr. Keto";
        final String url = "https://www.dietdoctor.com/low-carb/keto";
        final String content = "A keto or ketogenic diet is a very low-carb diet, " +
                "which turns the body into a fat-burning machine. It has many proven " +
                "benefits for weight loss, health and performance, as millions of" +
                " people have experienced already.";

        AddPostMutation.Data expected = new AddPostMutation.Data(new AddPostMutation.CreatePost(
                "Post",
                title,
                author,
                url,
                content
        ));

        AddPostMutation addPostMutation = AddPostMutation.builder()
                .input(CreatePostInput.builder()
                        .title(title)
                        .author(author)
                        .url(url)
                        .content(content)
                        .build()
                ).build();

        mAWSAppSyncClient
                .mutate(addPostMutation, expected)
                .enqueue(new GraphQLCall.Callback<AddPostMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
                        assertNotNull("CreatePost Response should not ne bull.", response.data());
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull final ApolloException e) {
                        assertNotNull(e);
                        e.printStackTrace();
                        mCountDownLatch.countDown();
                    }
                });

        try {
            mCountDownLatch.await();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    private void queryPosts(final ResponseFetcher responseFetcher) {

        final CountDownLatch queryCountDownLatch = new CountDownLatch(1);
        mAWSAppSyncClient.query(AllPostsQuery.builder().build())
                .responseFetcher(responseFetcher)
                .enqueue(new GraphQLCall.Callback<AllPostsQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull final Response<AllPostsQuery.Data> response) {
                        queryResponse = response;
                        queryCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        e.printStackTrace();
                        assertTrue("Exception thrown when querying posts." + e.getLocalizedMessage(), false);
                        queryCountDownLatch.countDown();
                    }
                });

        try {
            queryCountDownLatch.await();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }
}
