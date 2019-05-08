/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.compiler.ir

data class TypeDeclarationValue(
    val name: String,
    val description: String?,
    val isDeprecated: Boolean? = false,
    val deprecationReason: String? = null
)