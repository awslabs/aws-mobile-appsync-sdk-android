/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.test.runner.AndroidJUnit4;

import com.amazonaws.mobileconnectors.appsync.demo.AllArticlesQuery;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticle2Mutation;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.S3ObjectInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
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
        String title = "Thick as a brick";
        String author = "Tull @" + System.currentTimeMillis();
        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticleMutation.Data> createCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testAddComplexObject.pdf")
                    .localUri(filePath)
                    .mimeType("application/pdf")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .build())
            .build(),
            new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article", "", "", "", 1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
            ))
        )
        .enqueue(createCallback);

        Response<CreateArticleMutation.Data> createResponse = createCallback.awaitSuccessfulResponse();
        CreateArticleMutation.Data createData = createResponse.data();
        assertNotNull(createData);
        CreateArticleMutation.CreateArticle createArticle = createData.createArticle();
        assertNotNull(createArticle);
        assertNotNull(createArticle.id());
        CreateArticleMutation.Pdf pdf = createArticle.pdf();
        assertNotNull(pdf);
        assertEquals("testAddComplexObject.pdf", pdf.key());

        String updatedFilePath =
            appSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");

        LatchedGraphQLCallback<UpdateArticleMutation.Data> callback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(UpdateArticleMutation.builder()
            .input(UpdateArticleInput.builder()
                .id(createArticle.id())
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testUpdatedComplexObject.pdf")
                    .localUri(updatedFilePath)
                    .mimeType("application/pdf")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .build())
            .build(),
            new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article", "", "", "", 2,
                new UpdateArticleMutation.Pdf("","","",""),
                null
            ))
        )
        .enqueue(callback);
        Response<UpdateArticleMutation.Data> updateResponse = callback.awaitSuccessfulResponse();
        UpdateArticleMutation.Data updateData = updateResponse.data();
        assertNotNull(updateData);
        UpdateArticleMutation.UpdateArticle updateArticle = updateData.updateArticle();
        assertNotNull(updateArticle);
        assertNotNull(updateArticle.id());
        // assertEquals("testUpdatedComplexObject.pdf", data.updateArticle().pdf().key());
        assertEquals(2, updateArticle.version());
    }

    @Test
    public void testAddUpdateComplexObjectWithWrongAuthMode() {
        String articleId = "xyz";
        String title = "Thick as a brick";
        String author = "Tull @" + System.currentTimeMillis();

        String filePath =
            appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticleMutation.Data> createCallback = LatchedGraphQLCallback.instance();
        iamAWSAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testAddComplexObject.pdf")
                    .localUri(filePath)
                    .mimeType("application/pdf")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .build())
            .build(),
            new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article", "", "", "", 1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
            ))
        )
        .enqueue(createCallback);

        Response<CreateArticleMutation.Data> createResponse = createCallback.awaitResponse();
        assertNotNull(createResponse);
        assertNotNull(createResponse.errors());
        assertEquals(
            "Not Authorized to access createArticle on type Mutation",
            createResponse.errors().get(0).message()
        );
        assertNotNull(createResponse.data());
        assertNull(createResponse.data().createArticle());

        String updatedFilePath =
            appSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");

        LatchedGraphQLCallback<UpdateArticleMutation.Data> updateCallback = LatchedGraphQLCallback.instance();
        iamAWSAppSyncClient.mutate(UpdateArticleMutation.builder()
            .input(UpdateArticleInput.builder()
                .id(articleId)
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testUpdatedComplexObject.pdf")
                    .localUri(updatedFilePath)
                    .mimeType("application/pdf")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .build())
            .build(),
            new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article", "", "", "", 2,
                new UpdateArticleMutation.Pdf("","","",""),
                null
            ))
        )
        .enqueue(updateCallback);

        Response<UpdateArticleMutation.Data> updateResponse = updateCallback.awaitResponse();
        assertNotNull(updateResponse);
        assertEquals(
            "Not Authorized to access updateArticle on type Mutation",
            updateResponse.errors().get(0).message()
        );
        assertNotNull(updateResponse.data());
        assertNull(updateResponse.data().updateArticle());
    }

    @Test
    public void testAddComplexObjectBadBucket( ) {
        String title = "Thick as a brick";
        String author = "Tull-Bad Bucket";

        String filePath =
            appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticleMutation.Data> createCallback = LatchedGraphQLCallback.instance();
        iamAWSAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(S3ObjectInput.builder()
                    .bucket("dfadfdsfjeje")
                    .key("testAddComplexObject.pdf")
                    .localUri(filePath)
                    .mimeType("application/pdf")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .build())
            .build(),
            new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article", "", "", "", 1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("","","","")
            ))
        )
        .enqueue(createCallback);
        assertNotNull(createCallback.awaitFailure());
    }

    @Test
    public void testAddUpdateTwoComplexObjects( ) {
        String title = "Thick as a brick";
        String author = "Tull @" + System.currentTimeMillis();
        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticleMutation.Data> createCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testAddTwoComplexObjects.pdf")
                    .localUri(filePath)
                    .mimeType("application/pdf")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .image(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testAddTwoComplexObjects.png")
                    .localUri(filePath)
                    .mimeType("image/png")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .build())
            .build(),
            new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article", "", author, title, 1,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
            ))
        )
        .enqueue(createCallback);
        Response<CreateArticleMutation.Data> createResponse = createCallback.awaitSuccessfulResponse();
        CreateArticleMutation.Data data = createResponse.data();
        assertNotNull(data);
        CreateArticleMutation.CreateArticle createArticle = data.createArticle();
        assertNotNull(createArticle);
        String articleId = createArticle.id();
        assertNotNull(articleId);
        //assertEquals("testAddTwoComplexObjects.pdf", response.data().createArticle().pdf().key());
        //assertEquals("testAddTwoComplexObjects.png", response.data().createArticle().image().key());

        String updatedFilePath =
            appSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");

        LatchedGraphQLCallback<UpdateArticleMutation.Data> updateCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(UpdateArticleMutation.builder()
            .input(UpdateArticleInput.builder()
                .id(articleId)
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testUpdateTwoComplexObjects.pdf")
                    .localUri(updatedFilePath)
                    .mimeType("application/pdf")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .image(S3ObjectInput.builder()
                    .bucket(appSyncTestSetupHelper.getBucketName())
                    .key("testUpdateTwoComplexObjects.png")
                    .localUri(updatedFilePath)
                    .mimeType("image/png")
                    .region(appSyncTestSetupHelper.getS3Region())
                    .build())
                .build())
            .build(),
            new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article", "", "", "", 2,
                new UpdateArticleMutation.Pdf("","","",""),
                new UpdateArticleMutation.Image("","","","")
            ))
        )
        .enqueue(updateCallback);

        Response<UpdateArticleMutation.Data> updateResponse = updateCallback.awaitSuccessfulResponse();
        UpdateArticleMutation.Data updateData = updateResponse.data();
        assertNotNull(updateData);
        UpdateArticleMutation.UpdateArticle updateArticle = updateData.updateArticle();
        assertNotNull(updateArticle);
        assertNotNull(updateArticle.id());
        // assertEquals("testUpdateTwoComplexObjects.pdf", response.data().updateArticle().pdf().key());
        // assertEquals("testUpdateTwoComplexObjects.png", response.data().updateArticle().image().key());
        assertEquals(2, updateArticle.version());
    }

    @Test
    public void testAddComplexObjectWithCreateArticle2() {
        String title = "Thick as a brick";
        String author = "Tull";
        String filePath = appSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticle2Mutation.Data> callback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(CreateArticle2Mutation.builder()
            .author(author)
            .title(title)
            .version(1)
            .pdf(S3ObjectInput.builder()
                .bucket(appSyncTestSetupHelper.getBucketName())
                .key("testAddComplexObjectWithCreateArticle2.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(appSyncTestSetupHelper.getS3Region())
                .build())
            .build(),
            new CreateArticle2Mutation.Data(new CreateArticle2Mutation.CreateArticle2(
            "Article", "", author, title, 1,
                new CreateArticle2Mutation.Pdf("","","",""),
                null
            ))
        )
        .enqueue(callback);

        Response<CreateArticle2Mutation.Data> response = callback.awaitSuccessfulResponse();
        assertNotNull(response.data());
        assertNotNull(response.data().createArticle2());
        CreateArticle2Mutation.CreateArticle2 createArticle2 = response.data().createArticle2();
        assertNotNull(createArticle2);
        assertNotNull(createArticle2.id());
        CreateArticle2Mutation.Pdf pdf = createArticle2.pdf();
        assertNotNull(pdf);
        assertEquals("testAddComplexObjectWithCreateArticle2.pdf", pdf.key());
    }

    Map<String, Response<AllArticlesQuery.Data>> listArticles(
            AWSAppSyncClient awsAppSyncClient, ResponseFetcher responseFetcher) {
        final CountDownLatch cacheLatch = new CountDownLatch(1);
        final CountDownLatch networkLatch = new CountDownLatch(1);
        final Map<String, Response<AllArticlesQuery.Data>> responses = new HashMap<>();

        awsAppSyncClient.query(AllArticlesQuery.builder().build())
                .responseFetcher(responseFetcher)
                .enqueue(DelegatingGraphQLCallback.to(response -> {
                    if (response.fromCache()) {
                        responses.put("CACHE", response);
                        cacheLatch.countDown();
                    } else {
                        responses.put("NETWORK", response);
                        networkLatch.countDown();
                    }
                }, failure -> {
                    cacheLatch.countDown();
                    networkLatch.countDown();
                }));
        if (AppSyncResponseFetchers.NETWORK_ONLY.equals(responseFetcher) ||
                AppSyncResponseFetchers.CACHE_AND_NETWORK.equals(responseFetcher)) {
            Await.latch(networkLatch);
            assertNotNull(responses.get("NETWORK"));
        }
        if (AppSyncResponseFetchers.CACHE_ONLY.equals(responseFetcher) ||
                AppSyncResponseFetchers.CACHE_AND_NETWORK.equals(responseFetcher)) {
            Await.latch(cacheLatch);
            assertNotNull(responses.get("CACHE"));
        }
        return responses;
    }

    @SuppressWarnings("deprecation") // clearMutationQueue is deprecated. C'est la vie.
    @Test
    public void testListArticlesWithWrongAuthMode() {
        // Query Articles through IAM Client with AppSyncResponseFetchers.NETWORK_ONLY
        Response<AllArticlesQuery.Data> networkOnlyResponse =
            listArticles(iamAWSAppSyncClient, AppSyncResponseFetchers.NETWORK_ONLY)
                .get("NETWORK");
        assertNotNull(networkOnlyResponse);
        assertTrue(
            "ListArticles through IAM Client should throw error",
            networkOnlyResponse.hasErrors()
        );
        assertEquals(
            "Not Authorized to access listArticles on type Query",
            networkOnlyResponse.errors().get(0).message()
        );
        assertNotNull(networkOnlyResponse.data());
        assertNull(networkOnlyResponse.data().listArticles());

        // Query Articles through IAM Client with AppSyncResponseFetchers.CACHE_ONLY
        Response<AllArticlesQuery.Data> cacheOnlyResponse =
            listArticles(iamAWSAppSyncClient, AppSyncResponseFetchers.CACHE_ONLY)
                .get("CACHE");
        assertNotNull(cacheOnlyResponse);
        assertNotNull(cacheOnlyResponse.data());
        assertNull(cacheOnlyResponse.data().listArticles());

        // Query Articles through IAM Client with AppSyncResponseFetchers.CACHE_AND_NETWORK
        Response<AllArticlesQuery.Data> cacheAndNetworkResponse =
            listArticles(iamAWSAppSyncClient, AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .get("NETWORK");
        assertNotNull(cacheAndNetworkResponse);
        assertTrue(
            "ListArticles through IAM Client should throw error",
            cacheAndNetworkResponse.hasErrors()
        );
        assertEquals(
            "Not Authorized to access listArticles on type Query",
            cacheAndNetworkResponse.errors().get(0).message()
        );
        assertNotNull(cacheAndNetworkResponse.data());
        assertNull(cacheAndNetworkResponse.data().listArticles());

        iamAWSAppSyncClient.clearMutationQueue();
        iamAWSAppSyncClient.getStore().clearAll();
    }
}
