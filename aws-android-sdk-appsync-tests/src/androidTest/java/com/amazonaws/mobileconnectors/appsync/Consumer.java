/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

/**
 * A consumer of a value.
 * This is inspired by {@link java.util.function.Consumer},
 * which is only available after API 24.
 * @param <V> The type of value being consumed
 */
interface Consumer<V> {
    /**
     * Accept a value.
     * @param value Value being consumed
     */
    void accept(V value);
}
