
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

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.demo.CreateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

public class AWSAppSyncConflictManagementInstrumentationTest {

    private static final String TAG = AWSAppSyncConflictManagementInstrumentationTest.class.getSimpleName();

    private String articleID = null;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAddUpdateArticleNoConflict( ) {
        String title = "Thick as a brick";
        String author = "Tull" + System.currentTimeMillis();

        String articleID = addArticle(title,author,1);
        updateArticle(articleID, title, author + System.currentTimeMillis(), 1, 2);
    }


    @Test
    public void testAddUpdateArticleConflictDiscard( ) {
        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        final CountDownLatch addCountDownLatch = new CountDownLatch(1);
        final CountDownLatch updateCountDownLatch = new CountDownLatch(1);

        articleID = null;

        //The TestConflictResolver setup in AppSyncTestSetupHelper will fail mutation
        // if the title is set to ALWAYS DISCARD
        String title = "ALWAYS DISCARD";
        String author = "Tull @" + System.currentTimeMillis();

        String articleID = addArticle(title,author,100);

        UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(articleID)
                .title(title)
                .author(author)
                .expectedVersion(1)
                .build();

        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                2,
                null,
                null
        ));
        UpdateArticleMutation updateArticleMutation = UpdateArticleMutation.builder().input(updateArticleInput).build();

        awsAppSyncClient
                .mutate(updateArticleMutation, expectedData)
                .enqueue(new GraphQLCall.Callback<UpdateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<UpdateArticleMutation.Data> response) {
                        Log.d(TAG, "Success? Should not happen.");
                        assertTrue(false);
                        updateCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertTrue(e instanceof ConflictResolutionFailedException);
                        updateCountDownLatch.countDown();
                    }
                });

        try {
            assertTrue(updateCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        //Do some more transaction to make sure nothing is stuck
        testAddUpdateArticleConflictResolve();
    }

    @Test
    public void testAddUpdateArticleConflictResolve( ) {
        String title = "Hallowed Point";
        String author = "Seasons in the Abyss @" + System.currentTimeMillis();

        String articleID = addArticle(title, author + System.currentTimeMillis(),100);

        //Send expectedVersion as 2. The conflict resolution mechanism should update version to 101 and the test should pass.
        updateArticle(articleID, title, author + "@" + System.currentTimeMillis(),2, 101);
        //No conflict
        updateArticle(articleID, title, author + "@" + System.currentTimeMillis(),101, 102);

        //Send expectedVersion as 101. The conflict resolution mechanism should update version to 103 and the test should pass.
        updateArticle(articleID, title, author + "@" + System.currentTimeMillis(),101, 103);

        //Send expectedVersion as 110. The conflict resolution mechanism should update version to 104 and the test should pass.
        updateArticle(articleID, title, author + "@" + System.currentTimeMillis(),110, 104);

    }

    @Test
    public void testAddUpdateArticleConflictResolveWithAnotherConflict( ) {
        String title = "RESOLVE_CONFLICT_INCORRECTLY";
        String author = "Trivium @" + System.currentTimeMillis();

        String articleID = addArticle(title, author + System.currentTimeMillis(),100);

        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        final CountDownLatch updateCountDownLatch = new CountDownLatch(1);
        final UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(articleID)
                .title(title)
                .author(author)
                .expectedVersion(2)
                .build();

        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                2,
                null,
                null
        ));
        UpdateArticleMutation updateArticleMutation = UpdateArticleMutation.builder().input(updateArticleInput).build();

        awsAppSyncClient
                .mutate(updateArticleMutation, expectedData)
                .enqueue(new GraphQLCall.Callback<UpdateArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<UpdateArticleMutation.Data> response) {
                        assertTrue(response.hasErrors());
                        updateCountDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Failure " + e);
                        assertNotNull(e);
                        updateCountDownLatch.countDown();
                    }
                });


        try {
            assertTrue(updateCountDownLatch.await(60, TimeUnit.SECONDS));
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        //Do some more mutations to make sure nothing is stuck.
        testAddUpdateArticleConflictResolve();
    }

    private String addArticle( final String title, final String author, final int version) {

        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        final CountDownLatch addCountDownLatch = new CountDownLatch(1);
        final CountDownLatch updateCountDownLatch = new CountDownLatch(1);

        articleID = null;

        CreateArticleMutation.Data expected = new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article",
                "",
                "",
                "",
                version,
                null,
                null
        ));

        CreateArticleInput createArticleInput = CreateArticleInput.builder()
                .title(title)
                .author(author)
                .version(version)
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
        return articleID;
    }


    private void updateArticle(final String id, final String title, final String author, final int expectedVersion,
                               final int versionNumberAfterUpdate) {
        final AWSAppSyncClient awsAppSyncClient = AppSyncTestSetupHelper.createAppSyncClientWithIAM();
        assertNotNull(awsAppSyncClient);
        final CountDownLatch updateCountDownLatch = new CountDownLatch(1);
        final UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                .id(id)
                .title(title)
                .author(author)
                .expectedVersion(expectedVersion)
                .build();

        UpdateArticleMutation.Data expectedData = new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article",
                "",
                "",
                "",
                expectedVersion,
                null,
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
                        assertEquals(title, response.data().updateArticle().title());
                        assertEquals(author, response.data().updateArticle().author());
                        assertEquals(versionNumberAfterUpdate, response.data().updateArticle().version());
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
}
