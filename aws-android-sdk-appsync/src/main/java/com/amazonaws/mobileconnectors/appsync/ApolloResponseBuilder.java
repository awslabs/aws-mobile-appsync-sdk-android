/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.util.Log;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.response.OperationResponseParser;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

/**
 * Utility to build Apollo response type from the raw WebSocket messages for subscriptions.
 */
class ApolloResponseBuilder {
    private static final String TAG = ApolloResponseBuilder.class.getSimpleName();
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);

    private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
    private final ResponseNormalizer<Map<String, Object>> mapResponseNormalizer;

    ApolloResponseBuilder(
            Map<ScalarType, CustomTypeAdapter> customTypeAdapters,
            ResponseNormalizer<Map<String, Object>> mapResponseNormalizer) {
        this.customTypeAdapters = customTypeAdapters;
        this.mapResponseNormalizer = mapResponseNormalizer;
    }

    <D extends Operation.Data, T, V extends Operation.Variables> Response<T> buildResponse(String message, Subscription<D, T, V> subscription) {
        // Parse the response using OperationResponseParser
        ResponseBody messageBody = ResponseBody.create(message, MEDIA_TYPE);
        OperationResponseParser<D, T> parser = new OperationResponseParser<>(
            subscription,
            subscription.responseFieldMapper(),
            new ScalarTypeAdapters(customTypeAdapters),
            mapResponseNormalizer);

        Response<T> parsedResponse;
        try {
            parsedResponse = parser.parse(messageBody.source());
        } catch (IOException ioException) {
            throw new RuntimeException("Error constructing JSON object", ioException);
        }

        if (parsedResponse.hasErrors()) {
            Log.w(TAG, "Errors detected in parsed subscription message");
        }
        return parsedResponse;
    }
}
