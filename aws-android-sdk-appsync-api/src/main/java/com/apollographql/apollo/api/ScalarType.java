/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.api;

/**
 * Represents a custom GraphQL scalar type
 */
public interface ScalarType {
  String typeName();

  Class javaType();
}
