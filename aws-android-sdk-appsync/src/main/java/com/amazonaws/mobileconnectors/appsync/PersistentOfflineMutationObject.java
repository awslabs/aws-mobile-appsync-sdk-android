/**
 * Copyright 2018-2018 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
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
