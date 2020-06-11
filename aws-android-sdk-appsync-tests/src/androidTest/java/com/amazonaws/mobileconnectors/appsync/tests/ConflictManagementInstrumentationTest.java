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
import com.amazonaws.mobileconnectors.appsync.ConflictResolutionFailedException;
import com.amazonaws.mobileconnectors.appsync.client.LatchedGraphQLCallback;
import com.amazonaws.mobileconnectors.appsync.client.NoOpGraphQLCallback;
import com.amazonaws.mobileconnectors.appsync.demo.CreateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.CreateArticleInput;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;
import com.amazonaws.mobileconnectors.appsync.identity.CustomCognitoUserPool;
import com.apollographql.apollo.api.Response;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.amazonaws.mobileconnectors.appsync.util.InternetConnectivity.goOnline;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ConflictManagementInstrumentationTest {
    /**
     * We will do one add and 5 updates that try out the various paths of conflict
     * management. This function will exit once the add is completed and the
     * updates are queued, but not executed. This has the effect of populating
     * the persistent queue and exercising the persistent mutation execution flow
     * when one of the tests in this suite starts.
     */
    @BeforeClass
    public static void beforeAnyTests() {
        goOnline();

        String title = "Minstrel in the Gallery";
        String author = "Tull";

        CustomCognitoUserPool.setup();
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();

        LatchedGraphQLCallback<CreateArticleMutation.Data> createArticleCallback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(
            CreateArticleMutation.builder()
                .input(CreateArticleInput.builder()
                    .title(title)
                    .author(author)
                    .version(100)
                    .build()
                )
                .build(),
            new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article", "", "", "", 100, null, null
            ))
        )
        .enqueue(createArticleCallback);

        Response<CreateArticleMutation.Data> response = createArticleCallback.awaitSuccessfulResponse();
        assertNotNull(response);
        assertNotNull(response.data());
        CreateArticleMutation.CreateArticle createArticle = response.data().createArticle();
        assertNotNull(createArticle);
        assertNotNull(createArticle.id());

        String[] titles = {
            title + System.currentTimeMillis(),
            "RESOLVE_CONFLICT_INCORRECTLY",
            title + System.currentTimeMillis(),
            "ALWAYS DISCARD",
            title + System.currentTimeMillis()
        };
        for (String string : titles) {
            awsAppSyncClient.mutate(
                UpdateArticleMutation.builder()
                    .input(UpdateArticleInput.builder()
                        .id(createArticle.id())
                        .title(string)
                        .author(author)
                        .expectedVersion(1)
                        .build())
                    .build(),
                new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                    "Article", "", "", "", 1, null, null
                ))
            )
                .enqueue(NoOpGraphQLCallback.instance());
        }
    }

    @Test
    public void testAddUpdateArticleNoConflict() {
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();

        String title = "Thick as a brick";
        String author = "Tull" + System.currentTimeMillis();

        String articleID = addArticle(awsAppSyncClient,title,author,1);
        updateArticle(awsAppSyncClient, articleID, title, author + System.currentTimeMillis(), 1, 2);
    }

    @Test
    public void testAddUpdateArticleConflictDiscard() {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();

        // The TestConflictResolver setup in AppSyncTestSetupHelper will fail mutation
        // if the title is set to ALWAYS DISCARD
        String title = "ALWAYS DISCARD";
        String author = "Tull @" + System.currentTimeMillis();
        String articleId = addArticle(awsAppSyncClient,title,author,100);

        LatchedGraphQLCallback<UpdateArticleMutation.Data> callback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(
            UpdateArticleMutation.builder()
                .input(UpdateArticleInput.builder()
                    .id(articleId)
                    .title(title)
                    .author(author)
                    .expectedVersion(1)
                    .build())
                .build(),
            new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article", "", "", "", 2, null, null
            ))
        )
        .enqueue(callback);

        assertTrue(callback.awaitFailure() instanceof ConflictResolutionFailedException);

        //Do some more transaction to make sure nothing is stuck
        testAddUpdateArticleConflictResolve();
    }

    @Test
    public void testAddUpdateArticleConflictResolve( ) {
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();

        String title = "Hallowed Point";
        String author = "Seasons in the Abyss @" + System.currentTimeMillis();

        String articleId = addArticle(awsAppSyncClient, title, author + System.currentTimeMillis(),100);

        //Send expectedVersion as 2. The conflict resolution mechanism should update version to 101 and the test should pass.
        updateArticle(awsAppSyncClient, articleId, title, author + "@" + System.currentTimeMillis(),2, 101);

        //No conflict
        updateArticle(awsAppSyncClient, articleId, title, author + "@" + System.currentTimeMillis(),101, 102);

        //Send expectedVersion as 101. The conflict resolution mechanism should update version to 103 and the test should pass.
        updateArticle(awsAppSyncClient, articleId, title, author + "@" + System.currentTimeMillis(),101, 103);

        //Send expectedVersion as 110. The conflict resolution mechanism should update version to 104 and the test should pass.
        updateArticle(awsAppSyncClient, articleId, title, author + "@" + System.currentTimeMillis(),110, 104);
    }

    @Test
    public void testAddUpdateArticleConflictResolveWithAnotherConflict( ) {
        String title = "RESOLVE_CONFLICT_INCORRECTLY";
        String author = "Trivium @" + System.currentTimeMillis();
        AWSAppSyncClient awsAppSyncClient = AWSAppSyncClients.withAPIKEYFromAWSConfiguration();

        String articleId = addArticle(awsAppSyncClient, title, author + System.currentTimeMillis(),100);

        LatchedGraphQLCallback<UpdateArticleMutation.Data> callback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(UpdateArticleMutation.builder()
            .input(UpdateArticleInput.builder()
                .id(articleId)
                .title(title)
                .author(author)
                .expectedVersion(2)
                .build())
            .build(),
            new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article", "", "", "", 2, null, null
            ))
        )
        .enqueue(callback);

        assertNotNull(callback.awaitFailure());

        //Do some more mutations to make sure nothing is stuck.
        testAddUpdateArticleConflictResolve();
    }

    private String addArticle(
            AWSAppSyncClient awsAppSyncClient,
            String title,
            String author,
            int version) {
        LatchedGraphQLCallback<CreateArticleMutation.Data> callback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(CreateArticleMutation.builder()
            .input(CreateArticleInput.builder()
                .title(title)
                .author(author)
                .build())
            .build(),
            new CreateArticleMutation.Data(new CreateArticleMutation.CreateArticle(
                "Article", "", "", "", version, null, null
            ))
        )
        .enqueue(callback);

        Response<CreateArticleMutation.Data> response = callback.awaitSuccessfulResponse();
        CreateArticleMutation.Data data = response.data();
        assertNotNull(data);
        CreateArticleMutation.CreateArticle createArticle = data.createArticle();
        assertNotNull(createArticle);
        assertNotNull(createArticle.id());
        return createArticle.id();
    }

    private void updateArticle(
            AWSAppSyncClient awsAppSyncClient,
            String id,
            String title,
            String author,
            int expectedVersion,
            int versionNumberAfterUpdate) {
        LatchedGraphQLCallback<UpdateArticleMutation.Data> callback = LatchedGraphQLCallback.instance();
        awsAppSyncClient.mutate(
            UpdateArticleMutation.builder()
                .input(UpdateArticleInput.builder()
                    .id(id)
                    .title(title)
                    .author(author)
                    .expectedVersion(expectedVersion)
                    .build())
                .build(),
            new UpdateArticleMutation.Data(new UpdateArticleMutation.UpdateArticle(
                "Article", "", "", "", expectedVersion, null, null
            ))
        )
        .enqueue(callback);

        Response<UpdateArticleMutation.Data> response = callback.awaitSuccessfulResponse();
        assertNotNull(response);
        assertNotNull(response.data());
        UpdateArticleMutation.UpdateArticle updateArticle = response.data().updateArticle();
        assertNotNull(updateArticle);
        assertEquals(title, updateArticle.title());
        assertEquals(author, updateArticle.author());
        assertEquals(versionNumberAfterUpdate, updateArticle.version());
    }
}
