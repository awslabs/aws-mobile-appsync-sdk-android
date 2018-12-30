
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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.demo.CreateArticle2Mutation;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.S3ObjectInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAddUpdateComplexObject( ) {
        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
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
                0,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
        ));

        String filePath = AppSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput obj = S3ObjectInput.builder()
                .bucket(AppSyncTestSetupHelper.getBucketName())
                .key("testAddComplexObject.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(0)
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

        String updatedFilePath = AppSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");
        assertNotNull(filePath);
        S3ObjectInput updatedObj = S3ObjectInput.builder()
                .bucket(AppSyncTestSetupHelper.getBucketName())
                .key("testUpdatedComplexObject.pdf")
                .localUri(updatedFilePath)
                .mimeType("application/pdf")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();

        UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(articleID)
                .title(title)
                .author(author)
                .version(1)
                .pdf(updatedObj)
                .build();
        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                1,
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
                        assertNotNull(response.data());
                        assertNotNull(response.data().updateArticle());
                        assertNotNull (articleID = response.data().updateArticle().id());
                        assertEquals("testUpdatedComplexObject.pdf", response.data().updateArticle().pdf().key());
                        assertEquals(1, response.data().updateArticle().version());
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
        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        String title = "Thick as a brick";
        String author = "Tull-Bad Bucket";
        CreateArticleMutation.Data expected = new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article",
                "",
                "",
                "",
                0,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("","","","")
        ));

        String filePath = AppSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput obj = S3ObjectInput.builder()
                .bucket("dfadfdsfjeje")
                .key("testAddComplexObject.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(0)
                .pdf(obj)
                .build();

        CreateArticleMutation createArticleMutation = CreateArticleMutation.builder().input(createArticleInput).build();

        awsAppSyncClient
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
        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
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
                0,
                new CreateArticleMutation.Pdf("","","",""),
                new CreateArticleMutation.Image("", "", "", "")
        ));

        String filePath = AppSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput pdf = S3ObjectInput.builder()
                .bucket(AppSyncTestSetupHelper.getBucketName())
                .key("testAddTwoComplexObjects.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();

        S3ObjectInput image = S3ObjectInput.builder()
                .bucket(AppSyncTestSetupHelper.getBucketName())
                .key("testAddTwoComplexObjects.png")
                .localUri(filePath)
                .mimeType("image/png")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(0)
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
                        assertNotNull(response.data());
                        assertNotNull(response.data().createArticle());
                        assertNotNull (articleID = response.data().createArticle().id());
                        assertEquals("testAddTwoComplexObjects.pdf", response.data().createArticle().pdf().key());
                        assertEquals("testAddTwoComplexObjects.png", response.data().createArticle().image().key());
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

        String updatedFilePath = AppSyncTestSetupHelper.createDataFile("testFile2.txt", "This is the updated article file");
        assertNotNull(filePath);
        S3ObjectInput updatedObj = S3ObjectInput.builder()
                .bucket(AppSyncTestSetupHelper.getBucketName())
                .key("testUpdateTwoComplexObjects.pdf")
                .localUri(updatedFilePath)
                .mimeType("application/pdf")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();

        S3ObjectInput updatedImage = S3ObjectInput.builder()
                .bucket(AppSyncTestSetupHelper.getBucketName())
                .key("testUpdateTwoComplexObjects.png")
                .localUri(updatedFilePath)
                .mimeType("image/png")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();

        UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(articleID)
                .title(title)
                .author(author)
                .version(1)
                .pdf(updatedObj)
                .image(updatedImage)
                .build();
        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                1,
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
                        assertEquals("testUpdateTwoComplexObjects.pdf", response.data().updateArticle().pdf().key());
                        assertEquals("testUpdateTwoComplexObjects.png", response.data().updateArticle().image().key());
                        assertEquals(1, response.data().updateArticle().version());
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
    public void testAddComplexObjectWithCreateArticle2( ) {
        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        String title = "Thick as a brick";
        String author = "Tull";
        String filePath = AppSyncTestSetupHelper.createDataFile("testFile1.txt", "This is a test file");
        assertNotNull(filePath);
        S3ObjectInput pdf = S3ObjectInput.builder()
                .bucket(AppSyncTestSetupHelper.getBucketName())
                .key("testAddComplexObjectWithCreateArticle2.pdf")
                .localUri(filePath)
                .mimeType("application/pdf")
                .region(AppSyncTestSetupHelper.getS3Region())
                .build();


        CreateArticle2Mutation.Data expected  = new CreateArticle2Mutation.Data(new CreateArticle2Mutation.CreateArticle2(
                "Article",
                "",
                author,
                title,
                0,
                new CreateArticle2Mutation.Pdf("","","",""),
                null));

        CreateArticle2Mutation createArticle2Mutation = CreateArticle2Mutation.builder()
                .author(author)
                .title(title)
                .version(0)
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
                        assertNotNull (articleID = response.data().createArticle2().id());
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
}
