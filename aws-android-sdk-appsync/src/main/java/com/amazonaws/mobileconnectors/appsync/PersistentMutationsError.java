/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import javax.annotation.Nonnull;

/**
 * PersistentMutationsError.
 */

public class PersistentMutationsError {
    private Exception exception;
    private String mutationClassName;
    private String recordIdentifier;

    public PersistentMutationsError(@Nonnull String mutationClassName, @Nonnull String recordIdentifier, @Nonnull Exception exception) {
        this.exception = exception;
        this.mutationClassName = mutationClassName;
        this.recordIdentifier = recordIdentifier;
    }

    public Exception getException() {
        return this.exception;
    }

    public String getMutationClassName() {
        return this.mutationClassName;
    }

    public String getRecordIdentifier() {
        return this.recordIdentifier;
    }
}
