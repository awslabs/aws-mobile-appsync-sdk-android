/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.os.Message;
import android.util.Log;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.exception.ApolloParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * ConflictResolutionHandler.
 */

public class ConflictResolutionHandler {

    final AppSyncOfflineMutationInterceptor mutationInterceptor;
    private static final String TAG = ConflictResolutionHandler.class.getSimpleName();

    public ConflictResolutionHandler(@Nonnull AppSyncOfflineMutationInterceptor mutationInterceptor) {
        this.mutationInterceptor = mutationInterceptor;
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> void retryMutation(@Nonnull Mutation<D, T, V> mutation,
                                                                                         @Nonnull String identifier) {
        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Calling retry conflict mutation.");
        mutationInterceptor.retryConflictMutation(mutation, identifier);
    }

    public void fail(final String identifier) {
        mutationInterceptor.failConflictMutation(identifier);
    }

    static boolean conflictPresent(Optional<Response> parsedResponse) {

        //Check if the parsed response contains a conflict.
        //The contract for conflicts is that the response will contain an error with the
        //string "The conditional request failed" and will contain a data element.

        if (parsedResponse == null || parsedResponse.get() == null || parsedResponse.get().hasErrors() == false) {
            return false;
        }

        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: onResponse -- found error");

        if ( ! parsedResponse.get().errors().get(0).toString().contains("The conditional request failed") ) {
            return false;
        }

        Map customAttributes = ((Error) parsedResponse.get().errors().get(0)).customAttributes();
        if (customAttributes == null || customAttributes.get("data") == null ) {
            return false;
        }
       return true;
    }

    static boolean conflictPresent(String responseString) {
        try {
            if (responseString == null ) {
                return false;
            }
            JSONObject jsonObject = new JSONObject(responseString);
            JSONArray errors = jsonObject.optJSONArray("errors");
            if (errors == null || errors.length() < 1 ) {
                return false;
            }

            String errorType = errors.getJSONObject(0).optString("errorType");
            if (errorType == null ) {
                return false;
            }

            if (errorType.equals("DynamoDB:ConditionalCheckFailedException")) {
                return true;
            }
        }
        catch (JSONException e) {

        }
        return false;
    }
}
