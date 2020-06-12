/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api.internal;

/**
 * Represents an operation that accepts a single input argument, mutate it and returns no result
 *
 * @param <T> the type of the input to the operation
 */
public interface Mutator<T> {

  /**
   * Performs this operation on the given argument.
   *
   * @param t the input argument
   */
  void accept(T t);

}
