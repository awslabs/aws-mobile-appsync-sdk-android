/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.compiler.ir

data class Variable(
    val name: String,
    val type: String
) {
  fun optional(): Boolean = !type.endsWith(suffix = "!")
}