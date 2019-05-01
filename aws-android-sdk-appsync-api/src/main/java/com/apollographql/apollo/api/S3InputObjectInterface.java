/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.api;

/**
 * S3InputObjectInterface.
 */

public interface S3InputObjectInterface extends S3ObjectInterface {
    public String localUri();
    public String mimeType();
}
