/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.client;

import androidx.annotation.NonNull;

import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

/**
 * An {@link GraphQLCall.Callback} that does nothing.
 * @param <T> Type of data in the GraphQL response
 */
public final class NoOpGraphQLCallback<T> extends GraphQLCall.Callback<T> {
    private NoOpGraphQLCallback() {}

    public static <T> NoOpGraphQLCallback<T> instance() {
        return new NoOpGraphQLCallback<>();
    }

    @Override
    public void onResponse(@NonNull Response<T> response) {
    }

    @Override
    public void onFailure(@NonNull ApolloException failure) {
    }
}
