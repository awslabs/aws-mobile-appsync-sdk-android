/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.field;

import com.amazonaws.apollographql.apollo.api.ResponseField;

public interface FieldValueResolver<R> {
  <T> T valueFor(R recordSet, ResponseField field);
}
