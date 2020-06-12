/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.exception;

public class ApolloException extends Exception {

  public ApolloException(String message) {
    super(message);
  }

  public ApolloException(String message, Throwable cause) {
    super(message, cause);
  }
}
