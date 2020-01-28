/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.annotation.NonNull;

import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

/**
 * An {@link GraphQLCall.Callback} that does nothing.
 * @param <T> Type of data in the GraphQL response
 */
final class NoOpGraphQLCallback<T> extends GraphQLCall.Callback<T> {
    private NoOpGraphQLCallback() {}

    static <T> GraphQLCall.Callback<T> instance() {
        return new NoOpGraphQLCallback<>();
    }

    @Override
    public void onResponse(@NonNull Response<T> response) {
    }

    @Override
    public void onFailure(@NonNull ApolloException failure) {
    }
}
