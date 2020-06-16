/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.tests;

import android.support.test.runner.AndroidJUnit4;

import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.client.AWSAppSyncClients;
import com.amazonaws.mobileconnectors.appsync.client.DelegatingGraphQLCallback;
import com.amazonaws.mobileconnectors.appsync.client.LatchedGraphQLCallback;
import com.amazonaws.mobileconnectors.appsync.demo.AllArticlesQuery;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticle2Mutation;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.S3ObjectInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.identity.CustomCognitoUserPool;
import com.amazonaws.mobileconnectors.appsync.util.Await;
import com.amazonaws.mobileconnectors.appsync.util.DataFile;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.amazonaws.mobileconnectors.appsync.util.InternetConnectivity.goOnline;
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
public class ComplexObjectsInstrumentationTests {
    private static final String REGION = "us-west-2";
    private static final String BUCKET_NAME = "aws-appsync-tests-android163429-dev";

    private static AWSAppSyncClient awsAppSyncClient;
    private static AWSAppSyncClient iamAWSAppSyncClient;

    @BeforeClass
    public static void beforeAnyTests() {
        goOnline();
        CustomCognitoUserPool.setup();
        awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();
        iamAWSAppSyncClient = AWSAppSyncClients.withIAMFromAWSConfiguration();
    }

    @Test
    public void testAddUpdateComplexObject() {
        String title = "Thick as a brick";
        String author = "Tull @" + System.currentTimeMillis();
        String filePath = DataFile.create("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticleMutation.Data> createCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .pdf(S3ObjectInput.builder()
                    .bucket(BUCKET_NAME)
                    .key("uploads/testAddComplexObject.pdf")
                    .localUri(filePath)
                    .mimeType("application/pdf")
                    .region(REGION)
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
        assertEquals("uploads/testAddComplexObject.pdf", pdf.key());

        String updatedFilePath =
            DataFile.create("testFile2.txt", "This is the updated article file");

        LatchedGraphQLCallback<UpdateArticleMutation.Data> callback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(UpdateArticleMutation.builder()
            .input(UpdateArticleInput.builder()
                .id(createArticle.id())
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(BUCKET_NAME)
                    .key("uploads/testUpdatedComplexObject.pdf")
                    .localUri(updatedFilePath)
                    .mimeType("application/pdf")
                    .region(REGION)
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
    public void testAddComplexObjectBadBucket( ) {
        String title = "Thick as a brick";
        String author = "Tull-Bad Bucket";

        String filePath = DataFile.create("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticleMutation.Data> createCallback = LatchedGraphQLCallback.instance();
        iamAWSAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .pdf(S3ObjectInput.builder()
                    .bucket("dfadfdsfjeje")
                    .key("uploads/testAddComplexObject.pdf")
                    .localUri(filePath)
                    .mimeType("application/pdf")
                    .region(REGION)
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
        String filePath = DataFile.create("testFile1.txt", "This is a test file");

        LatchedGraphQLCallback<CreateArticleMutation.Data> createCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .pdf(S3ObjectInput.builder()
                    .bucket(BUCKET_NAME)
                    .key("uploads/testAddTwoComplexObjects.pdf")
                    .localUri(filePath)
                    .mimeType("application/pdf")
                    .region(REGION)
                    .build())
                .image(S3ObjectInput.builder()
                    .bucket(BUCKET_NAME)
                    .key("uploads/testAddTwoComplexObjects.png")
                    .localUri(filePath)
                    .mimeType("image/png")
                    .region(REGION)
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

        String updatedFilePath = DataFile.create("testFile2.txt", "This is the updated article file");

        LatchedGraphQLCallback<UpdateArticleMutation.Data> updateCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(UpdateArticleMutation.builder()
            .input(UpdateArticleInput.builder()
                .id(articleId)
                .title(title)
                .author(author)
                .expectedVersion(1)
                .pdf(S3ObjectInput.builder()
                    .bucket(BUCKET_NAME)
                    .key("uploads/testUpdateTwoComplexObjects.pdf")
                    .localUri(updatedFilePath)
                    .mimeType("application/pdf")
                    .region(REGION)
                    .build())
                .image(S3ObjectInput.builder()
                    .bucket(BUCKET_NAME)
                    .key("uploads/testUpdateTwoComplexObjects.png")
                    .localUri(updatedFilePath)
                    .mimeType("image/png")
                    .region(REGION)
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
}
