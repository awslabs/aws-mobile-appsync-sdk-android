/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.annotation.Nullable;

import com.amazonaws.mobile.client.Callback;

/**
 * An AWS Mobile Client {@link Callback} which passes any received result/error
 * along to a delegate consumer. For example, if this callback receives an Exception,
 * it will just invoke {@link Consumer#accept(T)} on the error consumer, to handle it.
 * That consumer can deal with the exception, from there on.
 * @param <T> Type of result received in the callback
 */
final class DelegatingCallback<T> implements Callback<T> {
    private final Consumer<T> onResult;
    private final Consumer<Exception> onError;

    private DelegatingCallback(Consumer<T> onResult, Consumer<Exception> onError) {
        this.onResult = onResult;
        this.onError = onError;
    }

    /**
     * Creates a delegating {@link Callback} for the AWS Mobile Client.
     * @param onResult Delegate consumer invoked when callback receives result
     * @param onError Delegate consumer invoked when callback receives exception
     * @param <T> Type of result data
     * @return An AWS Mobile Client callback which delegates values to one of two consumers.
     */
    static <T> DelegatingCallback<T> to(
            @Nullable Consumer<T> onResult, @Nullable Consumer<Exception> onError) {
        return new DelegatingCallback<>(
            onResult != null ? onResult : DefaultConsumer.<T>instance(),
            onError != null ? onError : DefaultConsumer.<Exception>instance()
        );
    }
    @Override
    public void onResult(T result) {
        onResult.accept(result);
    }

    @Override
    public void onError(Exception error) {
        onError.accept(error);
    }

    /**
     * A consumer which throws an exception upon being called to accept a value.
     * @param <V> The type of value accepted by the consumer.
     */
    static final class DefaultConsumer<V> implements Consumer<V> {
        private DefaultConsumer() {}

        @Override
        public void accept(V value) {
            throw new RuntimeException("A Consumer was not provided to DelegatingCallback!");
        }

        static <V> DefaultConsumer<V> instance() {
            return new DefaultConsumer<>();
        }
    }
}
