/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api;

import java.io.IOException;

/**
 * FragmentResponseFieldMapper is responsible for mapping the response back to a fragment of type T.
 */
public interface FragmentResponseFieldMapper<T> {
  T map(final ResponseReader responseReader, String conditionalType) throws IOException;
}
