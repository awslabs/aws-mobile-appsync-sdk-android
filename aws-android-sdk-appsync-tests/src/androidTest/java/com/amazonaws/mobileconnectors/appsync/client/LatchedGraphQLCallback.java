/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazonaws.mobileconnectors.appsync.util.Await;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link GraphQLCall.Callback} which contains
 * some utility methods, useful to block until results/errors arrive in
 * the callback. The family of awaitSomething() methods use {@link CountDownLatch}
 * to block until behaviors occur (or, until they for sure haven't, within a time period.)
 * @param <T> The type of data in the GraphQL response, arriving at callback
 */
@SuppressWarnings("unused")
public final class LatchedGraphQLCallback<T> extends GraphQLCall.Callback<T> {
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(10);

    private final CountDownLatch responseLatch;
    private final CountDownLatch failureLatch;
    private final AtomicReference<Response<T>> responseContainer;
    private final AtomicReference<ApolloException> failureContainer;
    private final long waitTimeMs;

    private LatchedGraphQLCallback(long waitTimeMs) {
        this.responseLatch = new CountDownLatch(1);
        this.failureLatch = new CountDownLatch(1);
        this.responseContainer = new AtomicReference<>();
        this.failureContainer = new AtomicReference<>();
        this.waitTimeMs = waitTimeMs;
    }

    /**
     * Creates an instance of a GraphQLCall.Callback which can be used
     * to block the calling thread until results/errors arrive in the callback.
     * The instance will use a reasonable timeout value for the latch.
     * @param <T> The type of the result in the callback
     * @return A latched GraphQLCall.Callback
     */
    @NonNull
    public static <T> LatchedGraphQLCallback<T> instance() {
        return new LatchedGraphQLCallback<>(REASONABLE_WAIT_TIME_MS);
    }

    /**
     * Creates an instance of a GraphQLCall.Callback which can be used
     * to block the calling thread until results/errors arrive in the callback.
     * The instance will block for the provided number of milliseconds.
     * @param <T> The type of the result in the callback
     * @return A latched GraphQLCall.Callback
     */
    @NonNull
    public static <T> LatchedGraphQLCallback<T> instance(long waitTimeMs) {
        return new LatchedGraphQLCallback<>(waitTimeMs);
    }

    @Override
    public void onResponse(@NonNull Response<T> response) {
        responseContainer.set(response);
        responseLatch.countDown();
    }

    @Override
    public void onFailure(@NonNull ApolloException failure) {
        failureContainer.set(failure);
        releaseLatches();
    }

    private void releaseLatches() {
        responseLatch.countDown();
        failureLatch.countDown();
    }

    /**
     * Wait for a response to arrive in the callback's response listener.
     * Waits up until the timeout elapses. At that point, throws an error.
     * @return The response that arrived in the callback
     * @throws RuntimeException If no response arrives before the timeout elapses
     */
    @Nullable
    public Response<T> awaitResponse() {
        Await.latch(responseLatch, waitTimeMs);
        return responseContainer.get();
    }

    /**
     * Wait for a response to arrive in the callback's response listener.
     * Waits up until the timeout elapses. At that point, throws an error.
     * Validates that the received response is non-null, does not contain errors,
     * and has non-null data.
     * @return A validated, successful response
     * @throws RuntimeException
     *          If no response is received before the timeout elapses, or if the response
     *          is null, or contains errors, or has null data
     */
    @NonNull
    public Response<T> awaitSuccessfulResponse() {
        Response<T> response = awaitResponse();
        if (response == null) {
            if (failureContainer.get() != null) {
                throw new RuntimeException("Unexpected failure. " + failureContainer.get(), failureContainer.get().getCause());
            }
            throw new RuntimeException("Null response.");
        } else if (response.hasErrors()) {
            throw new RuntimeException("Response has errors: " + response.errors());
        } else if (response.data() == null) {
            if (failureContainer.get() != null) {
                throw new RuntimeException("Unexpected failure. " + failureContainer.get(), failureContainer.get().getCause());
            }
            throw new RuntimeException("Null response data.");
        }
        return response;
    }

    /**
     * Wait for a failure to arrive in the failure listener.
     * Waits until the timeout elapses. At that point, throws an error.
     * @return The failure was received by the callback, if one was
     * @throws RuntimeException If the timeout elapses before a failure is received by callback
     */
    @Nullable
    public ApolloException awaitFailure() {
        Await.latch(failureLatch, waitTimeMs);
        return failureContainer.get();
    }
}
