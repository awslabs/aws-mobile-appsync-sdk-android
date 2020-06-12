/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.compiler.ir

import com.squareup.javapoet.TypeSpec

interface CodeGenerator {
  fun toTypeSpec(context: CodeGenerationContext): TypeSpec
}
