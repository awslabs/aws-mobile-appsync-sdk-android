/**
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

/**
 * Checked Exception thrown by AWSAppSyncClient.
 */
public class AWSAppSyncClientException extends Exception {
    /**
     * Default constructor.
     */
    public AWSAppSyncClientException() {
        super();
    }

    /**
     * @param message the exception message.
     */
    public AWSAppSyncClientException(final String message) {
        super(message);
    }

    /**
     * @param message the exception message.
     * @param cause the throwable object that contains the cause.
     */
    public AWSAppSyncClientException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause the throwable object that contains the cause
     */
    public AWSAppSyncClientException(final Throwable cause) {
        super(cause);
    }
}