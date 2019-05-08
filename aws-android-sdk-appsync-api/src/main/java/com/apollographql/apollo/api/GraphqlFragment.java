/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.api;

/**
 * Represents a GraphQL fragment
 */
public interface GraphqlFragment {

  /**
   * Returns marshaller to serialize fragment data
   *
   * @return {@link ResponseFieldMarshaller} to serialize fragment data
   */
  ResponseFieldMarshaller marshaller();
}
