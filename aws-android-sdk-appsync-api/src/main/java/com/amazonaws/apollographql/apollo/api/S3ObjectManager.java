/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api;

/**
 * S3ObjectManager.
 */
public interface S3ObjectManager {
    void upload(final S3InputObjectInterface s3Object) throws Exception;
    void download(final S3ObjectInterface s3Object, final String filePath) throws Exception;
}
