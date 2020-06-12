/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.exception;

public final class ApolloCanceledException extends ApolloException {

  public ApolloCanceledException(String message) {
    super(message);
  }

  public ApolloCanceledException(String message, Throwable cause) {
    super(message, cause);
  }
}
