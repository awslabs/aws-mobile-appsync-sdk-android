/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.util;

final class RetryStrategies {
    private RetryStrategies() {}

    @SuppressWarnings("SameParameterValue")
    static void linear(Retryable action, Condition condition, int maxAttempts, int secondsBetween) {
        int attemptsRemaining = maxAttempts;
        while (attemptsRemaining > 0) {
            if (condition.isMet()) {
                return;
            }
            action.call();
            --attemptsRemaining;
            Sleep.seconds(secondsBetween);
        }
        throw new IllegalStateException("Failed to meet condition.");
    }

    @FunctionalInterface
    interface Retryable {
        void call();
    }

    @FunctionalInterface
    interface Condition {
        boolean isMet();
    }
}
