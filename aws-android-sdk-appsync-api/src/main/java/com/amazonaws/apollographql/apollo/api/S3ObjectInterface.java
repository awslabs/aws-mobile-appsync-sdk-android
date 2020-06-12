/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api;

/**
 * S3ObjectInterface.
 */

public interface S3ObjectInterface {
    public String bucket();
    public String key();
    public String region();
}
