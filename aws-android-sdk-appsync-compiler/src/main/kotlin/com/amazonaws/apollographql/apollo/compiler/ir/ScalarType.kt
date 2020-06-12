/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.compiler.ir

sealed class ScalarType(val name: String) {
  object ID : ScalarType("ID")
  object STRING : ScalarType("String")
  object INT : ScalarType("Int")
  object BOOLEAN : ScalarType("Boolean")
  object FLOAT : ScalarType("Float")
  object AWS_TIMESTAMP : ScalarType("AWSTimestamp")
}
