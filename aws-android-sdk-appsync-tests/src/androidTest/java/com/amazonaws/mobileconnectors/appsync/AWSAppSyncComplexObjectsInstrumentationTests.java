/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.demo.AllArticlesQuery;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticle2Mutation;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.S3ObjectInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

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
@RunWith(AndroidJUnit4.class)
public class AWSAppSyncComplexObjectsInstrumentationTests {

    private static final String TAG = AWSAppSyncComplexObjectsInstrumentationTests.class.getSimpleName();

    private String articleID = null;

    private static AppSyncTestSetupHelper appSyncTestSetupHelper;
    private static AWSAppSyncClient awsAppSyncClient;
    private static AWSAppSyncClient iamAWSAppSyncClient;

    @BeforeClass
    public static void setUpBeforeClass() {
        appSyncTestSetupHelper = new AppSyncTestSetupHelper();
        awsAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithAPIKEYFromAWSConfiguration();
        iamAWSAppSyncClient = appSyncTestSetupHelper.createAppSyncClientWithIAMFromAWSConfiguration();
    }

    @Test
    public void testAddUpdateComplexObject() {
        assertNotNull(awsAppSyncClient);
        final CountDownLatch addCountDownLatch = new CountDownLatch(1);
        final CountDownLatch updateCountDownLatch = new CountDownLatch(1);

        articleID = null;

        String title = "Thick as a brick";
        String author = "Tull @" + System.currentTimeMillis();
        CreateArticleMutation.Data expected = new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article",
                "",
                "",
                "",
                1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
        ));

        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput obj = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testAddComplexObject.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(obj)
                .build();

        CreateArticleMutation createArticleMutation = CreateArticleMutation.builder().input(createArticleInput).build();

        awsAppSyncClient
                .mutate(createArticleMutation, expected)
                .enqueue(new GraphQLCall.Callback<CreateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<CreateArticleMutation.Data> response) {
                        Log.d(TAG, "Success");
                        assertNotNull(response);
                        Log.d(TAG, response.hasErrors() ? response.errors().get(0).toString() : "No Error");
                        assertFalse("Response should not have errors", response.hasErrors());
                        assertNotNull(response.data());
                        assertNotNull(response.data().createArticle());
                        assertNotNull (articleID = response.data().createArticle().id());
                        assertEquals("testAddComplexObject.pdf", response.data().createArticle().pdf().key());
                        addCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(false);
                        addCountDownLatch.countDown();
                    }
                });

        try {
            assertTrue(addCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        String updatedFilePath = appSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");
        assertNotNull(filePath);
        S3ObjectInput updatedObj = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testUpdatedComplexObject.pdf")
                .localUri(updatedFilePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(articleID)
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(updatedObj)
                .build();
        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                2,
                new UpdateArticleMutation.Pdf("","","",""),
                null
        ));
        UpdateArticleMutation updateArticleMutation = UpdateArticleMutation.builder().input(updateArticleInput).build();

        awsAppSyncClient
                .mutate(updateArticleMutation, expectedData)
                .enqueue(new GraphQLCall.Callback<UpdateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<UpdateArticleMutation.Data> response) {
                        Log.d(TAG, "Success");
                        assertNotNull(response);
                        Log.d(TAG, response.hasErrors() ? response.errors().get(0).toString() : "No Error");
                        assertFalse("Response should not have errors", response.hasErrors());
                        assertNotNull(response.data());
                        assertNotNull(response.data().updateArticle());
                        assertNotNull(articleID = response.data().updateArticle().id());
                        // assertEquals("testUpdatedComplexObject.pdf", response.data().updateArticle().pdf().key());
                        assertEquals(2, response.data().updateArticle().version());
                        updateCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(false);
                        updateCountDownLatch.countDown();
                    }
                });


        try {
            assertTrue(updateCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

    }

    @Test
    public void testAddUpdateComplexObjectWithWrongAuthMode() {
        assertNotNull(iamAWSAppSyncClient);
        final CountDownLatch addCountDownLatch = new CountDownLatch(1);
        final CountDownLatch updateCountDownLatch = new CountDownLatch(1);

        articleID = "xyz";

        String title = "Thick as a brick";
        String author = "Tull @" + System.currentTimeMillis();
        CreateArticleMutation.Data expected = new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article",
                "",
                "",
                "",
                1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
        ));

        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput obj = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testAddComplexObject.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(obj)
                .build();

        CreateArticleMutation createArticleMutation = CreateArticleMutation.builder().input(createArticleInput).build();

        iamAWSAppSyncClient
                .mutate(createArticleMutation, expected)
                .enqueue(new GraphQLCall.Callback<CreateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<CreateArticleMutation.Data> response) {
                        assertNotNull(response);
                        Log.d(TAG, response.hasErrors() ? response.errors().get(0).toString() : "No Error");
                        assertEquals("Not Authorized to access createArticle on type Mutation", response.errors().get(0).message());
                        assertNotNull(response.data());
                        assertNull(response.data().createArticle());
                        addCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(false);
                        addCountDownLatch.countDown();
                    }
                });

        try {
            assertTrue(addCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        String updatedFilePath = appSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");
        assertNotNull(filePath);
        S3ObjectInput updatedObj = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testUpdatedComplexObject.pdf")
                .localUri(updatedFilePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(articleID)
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(updatedObj)
                .build();
        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                2,
                new UpdateArticleMutation.Pdf("","","",""),
                null
        ));
        UpdateArticleMutation updateArticleMutation = UpdateArticleMutation.builder().input(updateArticleInput).build();

        iamAWSAppSyncClient
                .mutate(updateArticleMutation, expectedData)
                .enqueue(new GraphQLCall.Callback<UpdateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<UpdateArticleMutation.Data> response) {
                        assertNotNull(response);
                        Log.d(TAG, response.hasErrors() ? response.errors().get(0).toString() : "No Error");
                        assertEquals("Not Authorized to access updateArticle on type Mutation", response.errors().get(0).message());
                        assertNotNull(response.data());
                        assertNull(response.data().updateArticle());
                        updateCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(false);
                        updateCountDownLatch.countDown();
                    }
                });


        try {
            assertTrue(updateCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    @Test
    public void testAddComplexObjectBadBucket( ) {
        assertNotNull(iamAWSAppSyncClient);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        String title = "Thick as a brick";
        String author = "Tull-Bad Bucket";
        CreateArticleMutation.Data expected = new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article",
                "",
                "",
                "",
                1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("","","","")
        ));

        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput obj = S3ObjectInput.builder()
                .bucket("dfadfdsfjeje")
                .key("testAddComplexObject.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(obj)
                .build();

        CreateArticleMutation createArticleMutation = CreateArticleMutation.builder().input(createArticleInput).build();

        iamAWSAppSyncClient
                .mutate(createArticleMutation, expected)
                .enqueue(new GraphQLCall.Callback<CreateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<CreateArticleMutation.Data> response) {
                        Log.d(TAG, "Success");
                        assertTrue(false);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertNotNull(e);
                        countDownLatch.countDown();
                    }
                });

        try {
            assertTrue(countDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    @Test
    public void testAddUpdateTwoComplexObjects( ) {
        assertNotNull(awsAppSyncClient);
        final CountDownLatch addCountDownLatch = new CountDownLatch(1);
        final CountDownLatch updateCountDownLatch = new CountDownLatch(1);

        String title = "Thick as a brick";
        String author = "Tull @" + System.currentTimeMillis();
        CreateArticleMutation.Data expected = new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article",
                "",
                author,
                title,
                1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
        ));

        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput pdf = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testAddTwoComplexObjects.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        S3ObjectInput image = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testAddTwoComplexObjects.png")
                .localUri(filePath)
                .mimeType("image/png")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(pdf)
                .image(image)
                .build();

        CreateArticleMutation createArticleMutation = CreateArticleMutation.builder().input(createArticleInput).build();

        awsAppSyncClient
                .mutate(createArticleMutation, expected)
                .enqueue(new GraphQLCall.Callback<CreateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<CreateArticleMutation.Data> response) {
                        Log.d(TAG, "Success");
                        Log.d(TAG, "Success");
                        assertNotNull(response);
                        Log.d(TAG, response.hasErrors() ? response.errors().get(0).toString() : "No errors");
                        assertEquals(0, response.errors().size());
                        assertNotNull(response.data());
                        assertNotNull(response.data().createArticle());
                        assertNotNull (articleID = response.data().createArticle().id());
                        //assertEquals("testAddTwoComplexObjects.pdf", response.data().createArticle().pdf().key());
                        //assertEquals("testAddTwoComplexObjects.png", response.data().createArticle().image().key());
                        addCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(false);
                        addCountDownLatch.countDown();
                    }
                });

        try {
            assertTrue(addCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        String updatedFilePath = appSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");
        assertNotNull(filePath);
        S3ObjectInput updatedObj = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testUpdateTwoComplexObjects.pdf")
                .localUri(updatedFilePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        S3ObjectInput updatedImage = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testUpdateTwoComplexObjects.png")
                .localUri(updatedFilePath)
                .mimeType("image/png")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();

        UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(articleID)
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(updatedObj)
                .image(updatedImage)
                .build();
        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                2,
                new UpdateArticleMutation.Pdf("","","",""),
                new UpdateArticleMutation.Image("","","","")
        ));
        UpdateArticleMutation updateArticleMutation = UpdateArticleMutation.builder().input(updateArticleInput).build();

        awsAppSyncClient
                .mutate(updateArticleMutation, expectedData)
                .enqueue(new GraphQLCall.Callback<UpdateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<UpdateArticleMutation.Data> response) {
                        Log.d(TAG, "Success");
                        assertNotNull(response);
                        assertNotNull(response.data());
                        assertNotNull(response.data().updateArticle());
                        assertNotNull (articleID = response.data().updateArticle().id());
                       // assertEquals("testUpdateTwoComplexObjects.pdf", response.data().updateArticle().pdf().key());
                       // assertEquals("testUpdateTwoComplexObjects.png", response.data().updateArticle().image().key());
                        assertEquals(2, response.data().updateArticle().version());
                        updateCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(false);
                        updateCountDownLatch.countDown();
                    }
                });


        try {
            assertTrue(updateCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

    }

    @Test
    public void testAddComplexObjectWithCreateArticle2() {
        assertNotNull(awsAppSyncClient);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        String title = "Thick as a brick";
        String author = "Tull";
        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput pdf = S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testAddComplexObjectWithCreateArticle2.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build();


        CreateArticle2Mutation.Data expected  = new CreateArticle2Mutation.Data(new CreateArticle2Mutation.CreateArticle2(
                "Article",
                "",
                author,
                title,
                1,
                new CreateArticle2Mutation.Pdf("","","",""),
                null));

        CreateArticle2Mutation createArticle2Mutation = CreateArticle2Mutation.builder()
                .author(author)
                .title(title)
                .version(1)
                .pdf(pdf)
                .build();

        awsAppSyncClient
                .mutate(createArticle2Mutation, expected)
                .enqueue(new GraphQLCall.Callback<CreateArticle2Mutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<CreateArticle2Mutation.Data> response) {
                        Log.d(TAG, "Success");
                        assertNotNull(response);
                        assertNotNull(response.data());
                        assertNotNull(response.data().createArticle2());
                        assertNotNull(articleID = response.data().createArticle2().id());
                        assertEquals("testAddComplexObjectWithCreateArticle2.pdf", response.data().createArticle2().pdf().key());

                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(false);
                        countDownLatch.countDown();
                    }
                });

        try {
            assertTrue(countDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    Map<String, Response<AllArticlesQuery.Data>> listArticles(AWSAppSyncClient awsAppSyncClient,
                                                              final ResponseFetcher responseFetcher) {
        final CountDownLatch queryCountDownLatch;
        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            queryCountDownLatch = new CountDownLatch(2);
        } else {
            queryCountDownLatch = new CountDownLatch(1);
        }

        Log.d(TAG, "Calling Query listArticles");
        final Map<String, Response<AllArticlesQuery.Data>> responses = new HashMap<>();
        awsAppSyncClient.query(AllArticlesQuery.builder().build())
                .responseFetcher(responseFetcher)
                .enqueue(new GraphQLCall.Callback<AllArticlesQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<AllArticlesQuery.Data> response) {
                        if (response.fromCache()) {
                            responses.put("CACHE", response);
                        } else {
                            responses.put("NETWORK", response);
                        }
                        queryCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        e.printStackTrace();
                        queryCountDownLatch.countDown();
                    }
                });
        try {
            assertTrue(queryCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        if (responseFetcher.equals(AppSyncResponseFetchers.CACHE_AND_NETWORK)) {
            assertNotNull(responses.get("CACHE"));
            assertNotNull(responses.get("NETWORK"));
            assertEquals(2, responses.size());
        } else {
            assertEquals(1, responses.size());
        }

        return responses;
    }

    @Test
    public void testListArticlesWithWrongAuthMode() {
        final AWSAppSyncClient iamClientForArticles = iamAWSAppSyncClient;
        assertNotNull(iamClientForArticles);

        Response<AllArticlesQuery.Data> allArticlesResponse;

        // Query Articles through IAM Client with AppSyncResponseFetchers.NETWORK_ONLY
        allArticlesResponse = listArticles(iamClientForArticles, AppSyncResponseFetchers.NETWORK_ONLY).get("NETWORK");
        assertNotNull(allArticlesResponse);
        assertTrue("ListArticles through IAM Client should throw error",
                allArticlesResponse.hasErrors());
        assertEquals("Not Authorized to access listArticles on type Query",
                allArticlesResponse.errors().get(0).message());
        assertNotNull(allArticlesResponse.data());
        assertNull(allArticlesResponse.data().listArticles());

        // Query Articles through IAM Client with AppSyncResponseFetchers.CACHE_ONLY
        allArticlesResponse = listArticles(iamClientForArticles, AppSyncResponseFetchers.CACHE_ONLY).get("CACHE");
        assertNotNull(allArticlesResponse);
        assertNotNull(allArticlesResponse.data());
        assertNull(allArticlesResponse.data().listArticles());

        // Query Articles through IAM Client with AppSyncResponseFetchers.CACHE_AND_NETWORK
        allArticlesResponse = listArticles(iamClientForArticles, AppSyncResponseFetchers.CACHE_AND_NETWORK).get("NETWORK");
        assertNotNull(allArticlesResponse);
        assertTrue("ListArticles through IAM Client should throw error",
                allArticlesResponse.hasErrors());
        assertEquals("Not Authorized to access listArticles on type Query",
                allArticlesResponse.errors().get(0).message());
        assertNotNull(allArticlesResponse.data());
        assertNull(allArticlesResponse.data().listArticles());

        iamClientForArticles.clearMutationQueue();
        iamClientForArticles.getStore().clearAll();
    }
}
