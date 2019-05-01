/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.api;

/**
 * GraphQL operation name.
 */
public interface OperationName {
  /**
   * Returns operation name.
   *
   * @return operation name
   */
  String name();
}
