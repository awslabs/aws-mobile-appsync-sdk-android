/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.compiler

enum class NullableValueType(val value: String) {
  ANNOTATED("annotated"),
  APOLLO_OPTIONAL("apolloOptional"),
  GUAVA_OPTIONAL("guavaOptional"),
  JAVA_OPTIONAL("javaOptional"),
  INPUT_TYPE("inputType");

  companion object {
    fun findByValue(value: String): NullableValueType? = NullableValueType.values().find { it.value == value }
  }
}
