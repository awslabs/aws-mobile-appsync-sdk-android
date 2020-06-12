/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.compiler

import com.amazonaws.apollographql.apollo.compiler.ClassNames.parameterizedGuavaOptional
import com.amazonaws.apollographql.apollo.compiler.ClassNames.parameterizedInputType
import com.amazonaws.apollographql.apollo.compiler.ClassNames.parameterizedJavaOptional
import com.amazonaws.apollographql.apollo.compiler.ClassNames.parameterizedOptional
import com.amazonaws.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.amazonaws.apollographql.apollo.compiler.ir.ScalarType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

class JavaTypeResolver(
        private val context: CodeGenerationContext,
        private val packageName: String,
        private val deprecated: Boolean = false
) {
  fun resolve(typeName: String, isOptional: Boolean = !typeName.endsWith("!"),
      nullableValueType: NullableValueType? = null): TypeName {
    val normalizedTypeName = typeName.removeSuffix("!")
    val isList = normalizedTypeName.startsWith('[') && normalizedTypeName.endsWith(']')
    val customScalarType = context.customTypeMap[normalizedTypeName]
    val javaType = when {
      isList -> ClassNames.parameterizedListOf(resolve(normalizedTypeName.removeSurrounding("[", "]"), false))
      normalizedTypeName == ScalarType.STRING.name -> ClassNames.STRING
      normalizedTypeName == ScalarType.INT.name -> if (isOptional) TypeName.INT.box() else TypeName.INT
      normalizedTypeName == ScalarType.BOOLEAN.name -> if (isOptional) TypeName.BOOLEAN.box() else TypeName.BOOLEAN
      normalizedTypeName == ScalarType.FLOAT.name -> if (isOptional) TypeName.DOUBLE.box() else TypeName.DOUBLE
      normalizedTypeName == ScalarType.AWS_TIMESTAMP.name -> if (isOptional) TypeName.LONG.box() else TypeName.LONG
      customScalarType != null -> customScalarType.toJavaType()
      else -> ClassName.get(packageName, normalizedTypeName)
    }

    return if (javaType.isPrimitive) {
      javaType.let { if (deprecated) it.annotated(Annotations.DEPRECATED) else it }
    } else if (isOptional) {
      when (nullableValueType ?: context.nullableValueType) {
        NullableValueType.APOLLO_OPTIONAL -> parameterizedOptional(javaType)
        NullableValueType.GUAVA_OPTIONAL -> parameterizedGuavaOptional(javaType)
        NullableValueType.JAVA_OPTIONAL -> parameterizedJavaOptional(javaType)
        NullableValueType.INPUT_TYPE -> parameterizedInputType(javaType)
        else -> javaType.annotated(Annotations.NULLABLE)
      }.let {
        if (deprecated) it.annotated(Annotations.DEPRECATED) else it
      }
    } else {
      javaType.annotated(Annotations.NONNULL).let {
        if (deprecated) it.annotated(Annotations.DEPRECATED) else it
      }
    }
  }
}
