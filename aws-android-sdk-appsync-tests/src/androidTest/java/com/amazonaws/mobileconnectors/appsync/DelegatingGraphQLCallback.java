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
 * An {@link GraphQLCall.Callback} which works by passing any received values
 * to delegated {@link Consumer}s. For example, if the callback receives an
 * {@link Response}, it just passes it to the corresponding {@link Consumer} by calling
 * {@link Consumer#accept(T)}.
 */
final class DelegatingGraphQLCallback<T> extends GraphQLCall.Callback<T> {
    private final Consumer<Response<T>> onResponse;
    private final Consumer<ApolloException> onFailure;

    private DelegatingGraphQLCallback(
            @NonNull Consumer<Response<T>> onResponse,
            @NonNull Consumer<ApolloException> onFailure) {
        this.onResponse = onResponse;
        this.onFailure = onFailure;
    }

    /**
     * Creates a GraphQLCall.Callback which will delegate any receives response/failure
     * to one of two provided value consumers. The consumers can take it from there.
     * @param onResponse Consumer of GraphQL {@link Response}
     * @param onFailure Consumer of {@link ApolloException}
     * @param <T> Type of data in response
     * @return A delegating GraphQLCall.Callback
     */
    static <T> DelegatingGraphQLCallback<T> to(
            @NonNull Consumer<Response<T>> onResponse,
            @NonNull Consumer<ApolloException> onFailure) {
        return new DelegatingGraphQLCallback<>(onResponse, onFailure);
    }

    @Override
    public void onResponse(@NonNull Response<T> response) {
        onResponse.accept(response);
    }

    @Override
    public void onFailure(@NonNull ApolloException failure) {
        onFailure.accept(failure);
    }
}
