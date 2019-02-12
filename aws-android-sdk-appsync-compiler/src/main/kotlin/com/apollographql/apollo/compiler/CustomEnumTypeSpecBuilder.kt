/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.element.Modifier

class CustomEnumTypeSpecBuilder(
    val context: CodeGenerationContext
) {
  fun build(): TypeSpec =
      TypeSpec.enumBuilder(className(context))
          .addAnnotation(Annotations.GENERATED_BY_APOLLO)
          .addSuperinterface(ScalarType::class.java)
          .addModifiers(Modifier.PUBLIC)
          .addEnumConstants()
          .build()

  private fun TypeSpec.Builder.addEnumConstants(): TypeSpec.Builder {
    context.customTypeMap.forEach { mapping ->
      val constantName = mapping.key.removeSuffix("!").toUpperCase(Locale.ENGLISH)
      val javaTypeName = mapping.value
      addEnumConstant(constantName, scalarMappingTypeSpec(mapping.key, javaTypeName))
    }
    return this
  }

  private fun scalarMappingTypeSpec(scalarType: String, javaTypeName: String) =
      TypeSpec.anonymousClassBuilder("")
          .addMethod(MethodSpec.methodBuilder("typeName")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(java.lang.String::class.java)
              .addStatement("return \$S", scalarType)
              .build())
          .addMethod(MethodSpec.methodBuilder("javaType")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(Class::class.java)
              .addStatement("return \$T.class", javaTypeName.toJavaType())
              .build())
          .build()

  companion object {
    fun className(context: CodeGenerationContext): ClassName = ClassName.get(context.typesPackage, "CustomType")
  }

}