/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

/**
 * PersistentOfflineMutationObject.
 */

public class PersistentOfflineMutationObject {
    final String recordIdentifier;
    final String requestString;
    final String responseClassName;
    final String clientState;
    final String bucket;
    final String key;
    final String region;
    final String localURI;
    final String mimeType;

    public PersistentOfflineMutationObject(final String recordIdentifier,
                                           final String requestString,
                                           final String responseClassName,
                                           final String clientState) {
        this(recordIdentifier,
                requestString,
                responseClassName,
                clientState,
                null,
                null,
                null,
                null,
                null);
    }

    public PersistentOfflineMutationObject(final String recordIdentifier,
                                           final String requestString,
                                           final String responseClassName,
                                           final String clientState,
                                           final String bucket,
                                           final String key,
                                           final String region,
                                           final String localURI,
                                           final String mimeType) {
        this.recordIdentifier = recordIdentifier;
        this.requestString = requestString;
        this.responseClassName = responseClassName;
        this.clientState = clientState;
        this.bucket = bucket;
        this.key = key;
        this.region = region;
        this.localURI = localURI;
        this.mimeType = mimeType;
    }
}
