/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.annotation.NonNull;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A utility to await a value from an async function, in a synchronous way.
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue", "unused", "UnusedReturnValue"})
final class Await {
    private static final long DEFAULT_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(10);

    private Await() {}

    /**
     * Await a latch to count down.
     * @param latch Latch for which count down is awaited
     * @param waitTimeMs Time in milliseconds to wait for count down before timing out with exception
     * @throws RuntimeException If the latch doesn't count down in the specified amount of time
     */
    static void latch(CountDownLatch latch, long waitTimeMs) {
        try {
            latch.await(waitTimeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
        if (latch.getCount() != 0) {
            throw new RuntimeException("Latch did not count down.");
        }
    }

    /**
     * Await a latch to count down, for a "reasonable" amount of time.
     * Note: we choose what "reasonable" means. If you want to choose, use
     * {@link #latch(CountDownLatch, long)}, instead.
     * @param latch A count down latch for which countdown is awaited
     * @throws RuntimeException If the latch doesn't count down in a reasonable amount of time
     */
    static void latch(@NonNull CountDownLatch latch) {
        latch(latch, DEFAULT_WAIT_TIME_MS);
    }

    /**
     * Awaits emission of either a result of an error.
     * Blocks the thread of execution until either the value is
     * available, of a default timeout has elapsed.
     * @param resultErrorEmitter A function which emits result or error
     * @param <R> Type of result
     * @param <E> type of error
     * @return The result
     * @throws E if error is emitted
     * @throws RuntimeException In all other situations where there is not a non-null result
     */
    @NonNull
    static <R, E extends Throwable> R result(
        @NonNull ResultErrorEmitter<R, E> resultErrorEmitter) throws E {
        return result(DEFAULT_WAIT_TIME_MS, resultErrorEmitter);
    }

    /**
     * Await emission of either a result or an error.
     * Blocks the thread of execution until either the value is available,
     * or the timeout is reached.
     * @param timeMs Amount of time to wait
     * @param resultErrorEmitter A function which emits result or error
     * @param <R> Type of result
     * @param <E> Type of error
     * @return The result
     * @throws E if error is emitted
     * @throws RuntimeException In all other situations where there is not a non-null result
     */
    @NonNull
    static <R, E extends Throwable> R result(
        long timeMs, @NonNull ResultErrorEmitter<R, E> resultErrorEmitter) throws E {

        Objects.requireNonNull(resultErrorEmitter);

        AtomicReference<R> resultContainer = new AtomicReference<>();
        AtomicReference<E> errorContainer = new AtomicReference<>();

        await(timeMs, resultErrorEmitter, resultContainer, errorContainer);

        R result = resultContainer.get();
        E error = errorContainer.get();
        if (error != null) {
            throw error;
        } else if (result != null) {
            return result;
        }

        throw new IllegalStateException("Latch counted down, but where's the value?");
    }

    /**
     * Awaits receipt of an error or a callback.
     * Blocks the thread of execution until it arrives, or until the wait times out.
     * @param resultErrorEmitter An emitter of result of error
     * @param <R> Type of result
     * @param <E> Type of error
     * @return The error that was emitted by the emitter
     * @throws RuntimeException If no error was emitted by emitter
     */
    @NonNull
    public static <R, E extends Throwable> E error(@NonNull ResultErrorEmitter<R, E> resultErrorEmitter) {
        return error(DEFAULT_WAIT_TIME_MS, resultErrorEmitter);
    }

    /**
     * Awaits receipt of an error on an error callback.
     * Blocks the calling thread until it shows up, or until timeout elapses.
     * @param timeMs Amount of time to wait
     * @param resultErrorEmitter A function which emits result or error
     * @param <R> Type of result
     * @param <E> Type of error
     * @return Error, if attained
     * @throws RuntimeException If no error is emitted by the emitter
     */
    @NonNull
    static <R, E extends Throwable> E error(
            long timeMs, @NonNull ResultErrorEmitter<R, E> resultErrorEmitter) {

        Objects.requireNonNull(resultErrorEmitter);

        AtomicReference<R> resultContainer = new AtomicReference<>();
        AtomicReference<E> errorContainer = new AtomicReference<>();

        await(timeMs, resultErrorEmitter, resultContainer, errorContainer);

        R result = resultContainer.get();
        E error = errorContainer.get();
        if (result != null) {
            throw new RuntimeException("Expected error, but had result = " + result);
        } else if (error != null) {
            return error;
        }

        throw new RuntimeException("Neither error nor result consumers accepted a value.");
    }

    private static <R, E extends Throwable> void await(
            long timeMs,
            @NonNull final ResultErrorEmitter<R, E> resultErrorEmitter,
            @NonNull final AtomicReference<R> resultContainer,
            @NonNull final AtomicReference<E> errorContainer) {

        final CountDownLatch latch = new CountDownLatch(1);
        resultErrorEmitter.emitTo(
            new Consumer<R>() {
                @Override
                public void accept(R result) {
                    resultContainer.set(result);
                    latch.countDown();
                }
            }, new Consumer<E>() {
                @Override
                public void accept(E error) {
                    errorContainer.set(error);
                    latch.countDown();
                }
            }
        );

        try {
            latch.await(timeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            // Will check the latch count regardless, and branch appropriately.
        }
        if (latch.getCount() != 0) {
            throw new RuntimeException(
                "Neither result nor error consumers accepted a value within " + timeMs + "ms."
            );
        }
    }

    /**
     * A function which, upon completion, either emits a single result,
     * or emits an error.
     * @param <R> Type of result
     * @param <E> Type of error
     */
    interface ResultErrorEmitter<R, E extends Throwable> {
        /**
         * A function that emits a value upon completion, either as a
         * result or as an error.
         * @param onResult Callback invoked upon emission of result
         * @param onError Callback invoked upon emission of error
         */
        void emitTo(@NonNull Consumer<R> onResult, @NonNull Consumer<E> onError);
    }
}
