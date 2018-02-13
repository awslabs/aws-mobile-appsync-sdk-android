/**
 * Copyright 2018-2018 Amazon.com,
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

package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.JavaTypeResolver
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val possibleTypes: List<String>?,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) : CodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec =
      SchemaTypeSpecBuilder(
          typeName = formatClassName(),
          fields = fields,
          fragmentSpreads = fragmentSpreads ?: emptyList(),
          inlineFragments = emptyList(),
          context = context
      )
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .let {
            if (context.generateModelBuilder) {
              it.withBuilder()
            } else {
              it
            }
          }

  fun accessorMethodSpec(context: CodeGenerationContext): MethodSpec {
    return MethodSpec.methodBuilder(formatClassName().decapitalize())
        .addModifiers(Modifier.PUBLIC)
        .returns(typeName(context))
        .addStatement("return this.\$L", formatClassName().decapitalize())
        .build()
  }

  fun fieldSpec(context: CodeGenerationContext, publicModifier: Boolean = false): FieldSpec =
      FieldSpec.builder(typeName(context), formatClassName().decapitalize())
          .let { if (publicModifier) it.addModifiers(Modifier.PUBLIC) else it }
          .addModifiers(Modifier.FINAL)
          .build()

  fun formatClassName(): String = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  private fun typeName(context: CodeGenerationContext) = JavaTypeResolver(context, "").resolve(formatClassName(), true)

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}
