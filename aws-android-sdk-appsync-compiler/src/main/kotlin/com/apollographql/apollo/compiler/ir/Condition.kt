/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.compiler.ir

data class Condition(
    val kind: String,
    val variableName: String,
    val inverted: Boolean
) {

  enum class Kind(val rawValue: String) {
    BOOLEAN("BooleanCondition")
  }
}