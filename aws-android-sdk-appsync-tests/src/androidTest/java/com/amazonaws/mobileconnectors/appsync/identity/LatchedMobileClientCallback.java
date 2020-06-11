/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.identity;

import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobileconnectors.appsync.util.Await;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An AWS Mobile Client {@link Callback} which additionally has the ability
 * to block the calling thread of execution until a result/error is received.
 * @param <T> The type of result expected
 */
final class LatchedMobileClientCallback<T> implements Callback<T> {
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(10);

    private final CountDownLatch resultLatch;
    private final CountDownLatch errorLatch;
    private final AtomicReference<T> resultContainer;
    private final AtomicReference<Exception> errorContainer;
    private final long waitTimeMs;

    private LatchedMobileClientCallback(long waitTimeMs) {
        this.resultLatch = new CountDownLatch(1);
        this.resultContainer = new AtomicReference<>();
        this.errorLatch = new CountDownLatch(1);
        this.errorContainer = new AtomicReference<>();
        this.waitTimeMs = waitTimeMs;
    }

    /**
     * Creates a latched callback instance.
     * The latched callback can be used to await results, for a reasonable duration of time,
     * before timing out and throwing an error.
     * @param <T> Type of result provided to callback
     * @return A latched callback
     */
    static <T> LatchedMobileClientCallback<T> instance() {
        return new LatchedMobileClientCallback<>(REASONABLE_WAIT_TIME_MS);
    }

    /**
     * Creates a latched callback instance.
     * The latched callback can be used to await results, up until the provided timeout value
     * has elapsed. At that point, an error will be thrown.
     * @param waitTimeMs Amount of time to await results/failures
     * @param <T> Type of result
     * @return A latched callback
     */
    @SuppressWarnings("unused")
    static <T> LatchedMobileClientCallback<T> instance(long waitTimeMs) {
        return new LatchedMobileClientCallback<>(waitTimeMs);
    }

    @Override
    public void onResult(T result) {
        resultContainer.set(result);
        resultLatch.countDown();
    }

    @Override
    public void onError(Exception error) {
        errorContainer.set(error);
        errorLatch.countDown();
    }

    /**
     * Wait for a result to appear in the callback' result listener.
     * If the timeout elapses before a result is received, throws an error.
     * @return The result value
     */
    @SuppressWarnings("UnusedReturnValue")
    T awaitResult() {
        Await.latch(resultLatch, waitTimeMs);
        return resultContainer.get();
    }

    /**
     * Wait for an error to appear in the callback's error listener.
     * If the timeout elapses before an error is received, throws an error.
     * @return The exception that was received in the callback's error listener
     */
    @SuppressWarnings("unused")
    Exception awaitError() {
        Await.latch(errorLatch, waitTimeMs);
        return errorContainer.get();
    }
}
