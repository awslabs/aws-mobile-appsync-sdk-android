/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.exception;

public final class ApolloNetworkException extends ApolloException {
  public ApolloNetworkException(String message) {
    super(message);
  }

  public ApolloNetworkException(String message, Throwable cause) {
    super(message, cause);
  }
}
