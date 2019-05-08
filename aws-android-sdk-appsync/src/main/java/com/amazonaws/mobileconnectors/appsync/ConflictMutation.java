/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import javax.annotation.Nonnull;

/**
 * ConflictMutation.
 */

public class ConflictMutation {
    final String mutationId;
    final int retryCount;

    public ConflictMutation(@Nonnull String mutationId, @Nonnull int retryCount) {
        this.mutationId = mutationId;
        this.retryCount = retryCount;
    }
}
