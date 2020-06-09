/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link AppSyncSubscriptionCall.Callback} that additionally can be used to block
 * the calling thread until values show up in the callback.
 * @param <T>
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue", "WeakerAccess"})
final class LatchedSubscriptionCallback<T> implements AppSyncSubscriptionCall.Callback<T> {
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(10);

    private final CountDownLatch failureLatch;
    private final CountDownLatch completionLatch;
    private final AtomicReference<ApolloException> failureContainer;
    private final long waitTimeMs;

    private final List<Response<T>> responses;
    private CountDownLatch responseLatch;

    private LatchedSubscriptionCallback(long waitTimeMs) {
        this.failureLatch = new CountDownLatch(1);
        this.completionLatch = new CountDownLatch(1);
        this.failureContainer = new AtomicReference<>();
        this.waitTimeMs = waitTimeMs;
        this.responses = new ArrayList<>();
    }

    /**
     * Creates an instance of the callback that uses a default "reasonable" timeout
     * value for the await methods.
     * @param <T> The type of value expected in the subscription items
     * @return A latched subscription callback
     */
    static <T> LatchedSubscriptionCallback<T> instance() {
        return new LatchedSubscriptionCallback<>(REASONABLE_WAIT_TIME_MS);
    }

    /**
     * Creates an instance of the callback that uses the provided timeout value
     * for the await methods.
     * @param waitTimeMs Amount of time to await response/completion/failure when calling
     *                   the await family of methods
     * @param <T> Type of data expected in the stream of subscription responses.
     * @return A latched subscription callback
     */
    static <T> LatchedSubscriptionCallback<T> instance(long waitTimeMs) {
        return new LatchedSubscriptionCallback<>(waitTimeMs);
    }

    @Override
    public void onResponse(@NonNull Response<T> response) {
        responses.add(response);
        if (responseLatch != null) {
            responseLatch.countDown();
        }
    }

    @Override
    public void onFailure(@NonNull ApolloException failure) {
        failureContainer.set(failure);
        failureLatch.countDown();
    }

    @Override
    public void onCompleted() {
        completionLatch.countDown();
    }

    /**
     * Wait for a number of responses to arrive on the subscription. Some or all of these
     * responses may have arrives before this call is made. They will be buffered, until one
     * of the awaitResponse() style methods is called to retrieve them. If there are fewer
     * than the requested number of responses pending, then this call will block the thread of
     * execution until the remaining number of responses arrive. If there are still not enough
     * responses to satisfy the amount requested, by the time the timeout elapses, then an error
     * is raised.
     * @param desiredQuantity Number of resposnes being awaited
     * @return The requested number of responses
     * @throws RuntimeException If unable to produce the requested number of repsonses within the
     *                          timeout window, for any reason(s).
     */
    List<Response<T>> awaitResponses(int desiredQuantity) {
        // If we haven't yet received the desired quantity of responses on the subscription,
        // setup a latch to await the desired quantity, less the number of responses.
        // For example: I desire 5, I already have 3, I wait for 2 more.
        if (responses.size() < desiredQuantity) {
            responseLatch = new CountDownLatch(desiredQuantity - responses.size());
            Await.latch(responseLatch, waitTimeMs);
            responseLatch = null;
        }

        // If we already had the responses,
        // or if our latch counted down as a result of receiving the missing ones,
        // return the requested number of responses.
        List<Response<T>> returning = new ArrayList<>(responses.subList(0, desiredQuantity));

        // Also, clear these out of the responses list so that next call to awaitResponses()
        // returns (a) unique value(s).
        Iterator<Response<T>> iterator = responses.iterator();
        while (iterator.hasNext()) {
            if (returning.contains(iterator.next())) {
                iterator.remove();
            }
        }

        return returning;
    }

    /**
     * Awaits the receipt of a requested number of responses. Some or all of these responses may
     * have arrived before this call is made, and will have been buffered. If there are not enough
     * buffered responses available to satisfy the requested number (including, if there are none buffered
     * when this is called), then this call will block until the remaining quantity of responses shows up.
     * If the requested number of responses are not available within the timeout, an error is raised.
     * Once the requested number of respones _are_ available before the timeout, they are validated.
     * If _any_ response is not "successful," i.e., is null, has GraphQL errors, or contains null data,
     * then this call will throw an error.
     * @param desiredQuantity The number of successful responses desired
     * @return A list of successful responses of the requested size
     * @throws RuntimeException If unable to produce the requested number of responses, or if any
     *                          of the buffered responses are null, containing GraphQL errors, or null data
     */
    List<Response<T>> awaitSuccessfulResponses(int desiredQuantity) {
        awaitResponses(desiredQuantity);
        for (int pos = 0; pos < responses.size(); pos++) {
            Response<T> response = responses.get(pos);
            try {
                requireValidResponse(response);
            } catch (RuntimeException validationError) {
                throw new RuntimeException("Bad response at position " + pos + ".", validationError);
            }
        }
        return responses;
    }

    /**
     * Wait for the next response to show up on the subscription. If a response arrived before
     * this is called, it is returned, and cleared from the buffer. If no response is pending,
     * this call will block the thread of execution until the next response receives, or until
     * a timeout elapses. When the timeout elapses, an error is raised.
     * @return The next response that was found on the subscription
     */
    Response<T> awaitNextResponse() {
        return awaitResponses(1).get(0);
    }

    /**
     * Wait for a next response to be available. Either one was recently received
     * (before this is called), or this call will wait until one is. If a next response
     * is not available before the timeout elapses, an error is thrown. The next response
     * must also be a successful, valid response: non-null, having no GraphQL errors, and containing
     * non-null data.
     * @return The next validated successful response that is found on the subscription
     * @throws RuntimeException If the timeout elapses before a response arrives on the subscription,
     *                          or if the next repsonse that arrives is not a valid, non-null
     *                          successful response
     */
    Response<T> awaitNextSuccessfulResponse() {
        return requireValidResponse(awaitNextResponse());
    }

    /**
     * Expect that _no_ response will arrive on the callback before the timeout elapses.
     * If a response arrives before the timeout elapses, throws an error.
     * Note that this method lasts a duration of time _at least_ as long as the timeout value.
     * @throws RuntimeException If a value is received before the timeout elapses
     */
    void expectNoResponse() {
        Response<T> unexpectedResponse;
        try {
            unexpectedResponse = awaitNextResponse();
        } catch (RuntimeException errorFromNoResponse) {
            // 'S'all good, bruhs. This is what we wanted.
            return;
        }
        throw new RuntimeException("Received responses, but didn't want any. Response was: " + unexpectedResponse);
    }

    /**
     * Wait for a failure to be received in the failure callback.
     * If no failure is received before the timeout, an error is thrown.
     * @return The failure that was received by the callback
     * @throws RuntimeException If no failure is received before the timeout elapses
     */
    ApolloException awaitFailure() {
        Await.latch(failureLatch, waitTimeMs);
        return failureContainer.get();
    }

    /**
     * Wait for the subscription to receive a completion callback.
     * If no completion callback is received before the timeout, an error is thrown.
     * @throws RuntimeException If timeout elapses before completion callback occurs
     */
    void awaitCompletion() {
        Await.latch(completionLatch, waitTimeMs);
    }

    /**
     * Require that the provided response be "valid", that is, non-null,
     * having no errors, and containing non-null data.
     * @param response A response, possibly null, possibly containing errors, possibly having null data
     * @return A validated non-null response, containing no errors and with non-null data inside
     */
    @NonNull
    private Response<T> requireValidResponse(@Nullable Response<T> response) {
        if (response == null) {
            throw new RuntimeException("Response is null.");
        } else if (response.hasErrors()) {
            throw new RuntimeException("Response has errors: " + response.errors());
        } else if (response.data() == null) {
            throw new RuntimeException("Response data is null.");
        }
        return response;
    }
}
