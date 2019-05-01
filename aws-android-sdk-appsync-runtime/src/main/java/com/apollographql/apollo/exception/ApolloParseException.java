/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.exception;

public final class ApolloParseException extends ApolloException {

  public ApolloParseException(String message) {
    super(message);
  }

  public ApolloParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
