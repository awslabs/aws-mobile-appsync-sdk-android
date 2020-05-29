/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import java.util.concurrent.TimeUnit;

/**
 * A utility to sleep the calling thread.
 * The {@link InterruptedException} thrown by {@link Thread#sleep(long)} is wrapped into an
 * {@link RuntimeException} which helps cut down on test code clutter.
 */
final class Sleep {
    private Sleep() {}

    /**
     * Sleeps the current thread for a duration of milliseconds.
     * @param milliseconds Duration of time to sleep
     * @throws RuntimeException If unable to sleep for the requested amount of time
     */
    static void milliseconds(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }

    /**
     * Sleeps the current thread for a duration of seconds.
     * @param seconds Duration of time to sleep
     * @throws RuntimeException If unable to sleep for the requested amount of time
     */
    static void seconds(long seconds) {
        milliseconds(TimeUnit.SECONDS.toMillis(seconds));
    }
}
