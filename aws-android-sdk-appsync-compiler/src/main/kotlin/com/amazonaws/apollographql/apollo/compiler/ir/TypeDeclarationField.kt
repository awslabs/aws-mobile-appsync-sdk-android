/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.compiler.ir

data class TypeDeclarationField(
    val name: String,
    val description: String,
    val type: String,
    val defaultValue: Any?
)
