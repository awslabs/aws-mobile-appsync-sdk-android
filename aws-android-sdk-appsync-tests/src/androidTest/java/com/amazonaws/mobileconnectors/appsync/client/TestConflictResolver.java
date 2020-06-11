/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.client;

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.ConflictResolutionHandler;
import com.amazonaws.mobileconnectors.appsync.ConflictResolverInterface;
import com.amazonaws.mobileconnectors.appsync.demo.UpdateArticleMutation;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdateArticleInput;

import org.json.JSONException;
import org.json.JSONObject;

final class TestConflictResolver implements ConflictResolverInterface {
    private static final String TAG = TestConflictResolver.class.getSimpleName();

    @SuppressWarnings({"NullableProblems", "ConstantConditions"})
    @Override
    public void resolveConflict(
            ConflictResolutionHandler handler,
            JSONObject serverState,
            JSONObject clientState,
            String recordIdentifier,
            String operationType) {
        Log.v(TAG, "OperationType is [" + operationType + "]");
        if (operationType.equalsIgnoreCase("UpdateArticleMutation")) {
            try {
                if (clientState == null ) {
                    Log.v(TAG, "Failing conflict as client state was null");
                    handler.fail(recordIdentifier);
                    return;
                }

                //For the purposes of this test conflict handler
                //we will fail mutations if the title is ALWAYS DISCARD.
                JSONObject input = clientState.getJSONObject("input");
                if (input == null ) {
                    Log.v(TAG, "Failing conflict as input was null");
                    handler.fail(recordIdentifier);
                    return;
                }

                final String title = input.getString("title");
                if ("ALWAYS DISCARD".equals(title)) {
                    Log.v(TAG, "Failing conflict as title was ALWAYS DISCARD");
                    handler.fail(recordIdentifier);
                    return;
                }

                String id = input.getString("id");
                String author = input.getString("author");

                if (id == null || author == null || title == null ) {
                    Log.v(TAG, "Failing conflict as id, author or title was null");
                    handler.fail(recordIdentifier);
                }
                int resolvedVersion = serverState.getInt("version");
                if ("RESOLVE_CONFLICT_INCORRECTLY".equals(title)) {
                    //This will fail again.
                    Log.v(TAG, "Resolving conflict incorrectly");
                    resolvedVersion = resolvedVersion - 1;
                }
                else {
                    Log.v(TAG, "Resolving conflict correctly");
                }

                UpdateArticleInput updateArticleInput = UpdateArticleInput.builder()
                    .id(id)
                    .title(title)
                    .author(author)
                    .expectedVersion(resolvedVersion)
                    .build();

                UpdateArticleMutation updateArticleMutation = UpdateArticleMutation.builder().input(updateArticleInput).build();
                handler.retryMutation(updateArticleMutation, recordIdentifier);
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
                // in case of un-expected errors, we fail the mutation
                // we can also call the below method if we want server data to be accepted instead of client.
                handler.fail(recordIdentifier);
            }
        }
        else {
            handler.fail(recordIdentifier);
        }
    }
}
