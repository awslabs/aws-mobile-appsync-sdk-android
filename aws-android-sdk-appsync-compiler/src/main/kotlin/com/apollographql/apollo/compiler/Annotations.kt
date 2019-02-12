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

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import javax.annotation.Generated
import javax.annotation.Nonnull
import javax.annotation.Nullable

object Annotations {
  val NULLABLE: AnnotationSpec = AnnotationSpec.builder(Nullable::class.java).build()
  val NONNULL: AnnotationSpec = AnnotationSpec.builder(Nonnull::class.java).build()
  val OVERRIDE: AnnotationSpec = AnnotationSpec.builder(Override::class.java).build()
  val GENERATED_BY_APOLLO: AnnotationSpec = AnnotationSpec.builder(Generated::class.java)
      .addMember("value", CodeBlock.of("\$S", "Apollo GraphQL")).build()
  val DEPRECATED: AnnotationSpec = AnnotationSpec.builder(java.lang.Deprecated::class.java).build()
}