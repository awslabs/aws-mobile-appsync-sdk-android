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

import com.apollographql.apollo.api.*
import com.apollographql.apollo.api.internal.Mutator
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder
import com.apollographql.apollo.api.internal.Utils
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.util.*

object ClassNames {
  val OBJECT: ClassName = ClassName.get(Object::class.java)
  val STRING: ClassName = ClassName.get(String::class.java)
  val LIST: ClassName = ClassName.get(List::class.java)
  val ARRAY_LIST: ClassName = ClassName.get(ArrayList::class.java)
  val GRAPHQL_OPERATION: ClassName = ClassName.get(Operation::class.java)
  val GRAPHQL_QUERY: ClassName = ClassName.get(Query::class.java)
  val GRAPHQL_MUTATION: ClassName = ClassName.get(Mutation::class.java)
  val GRAPHQL_SUBSCRIPTION: ClassName = ClassName.get(Subscription::class.java)
  val GRAPHQL_OPERATION_VARIABLES: ClassName = ClassName.get("", "${GRAPHQL_OPERATION.simpleName()}.Variables")
  val ILLEGAL_STATE_EXCEPTION: TypeName = ClassName.get(IllegalStateException::class.java)
  val MAP: ClassName = ClassName.get(Map::class.java)
  val HASH_MAP: ClassName = ClassName.get(HashMap::class.java)
  val UNMODIFIABLE_MAP_BUILDER: ClassName = ClassName.get(UnmodifiableMapBuilder::class.java)
  val OPTIONAL: ClassName = ClassName.get(Optional::class.java)
  val GUAVA_OPTIONAL: ClassName = ClassName.get("com.google.common.base", "Optional")
  val JAVA_OPTIONAL: ClassName = ClassName.get("java.util", "Optional")
  val API_UTILS: ClassName = ClassName.get(Utils::class.java)
  val FRAGMENT: ClassName = ClassName.get(GraphqlFragment::class.java)
  val INPUT: ClassName = ClassName.get(Input::class.java)
  val BUILDER: ClassName = ClassName.get("", "Builder")
  val MUTATOR: ClassName = ClassName.get(Mutator::class.java)
  var S3ObjectInput: ClassName = ClassName.get(S3InputObjectInterface::class.java)
  var S3Object: ClassName = ClassName.get(S3ObjectInterface::class.java)
  val INPUT_TYPE: ClassName = ClassName.get(InputType::class.java)

  fun <K : Any> parameterizedListOf(type: Class<K>): TypeName =
      ParameterizedTypeName.get(LIST, ClassName.get(type))

  fun parameterizedListOf(typeArgument: TypeName): TypeName =
      ParameterizedTypeName.get(LIST, typeArgument.let { if (it.isPrimitive) it.box() else it.withoutAnnotations() })

  fun <K : Any, V : Any> parameterizedMapOf(keyTypeArgument: Class<K>, valueTypeArgument: Class<V>): TypeName =
      ParameterizedTypeName.get(MAP, ClassName.get(keyTypeArgument).withoutAnnotations(),
          ClassName.get(valueTypeArgument).withoutAnnotations())

  fun <K : Any, V : Any> parameterizedHashMapOf(keyTypeArgument: Class<K>, valueTypeArgument: Class<V>): TypeName =
      ParameterizedTypeName.get(HASH_MAP, ClassName.get(keyTypeArgument).withoutAnnotations(),
          ClassName.get(valueTypeArgument).withoutAnnotations())

  fun <K : Any, V : Any> parameterizedUnmodifiableMapBuilderOf(keyTypeArgument: Class<K>,
      valueTypeArgument: Class<V>): TypeName =
      ParameterizedTypeName.get(UNMODIFIABLE_MAP_BUILDER, ClassName.get(keyTypeArgument).withoutAnnotations(),
          ClassName.get(valueTypeArgument).withoutAnnotations())

  fun <K : Any> parameterizedOptional(type: Class<K>): TypeName =
      ParameterizedTypeName.get(OPTIONAL, ClassName.get(type))

  fun parameterizedOptional(type: TypeName): TypeName =
      ParameterizedTypeName.get(OPTIONAL, type)

  fun parameterizedGuavaOptional(type: TypeName): TypeName =
      ParameterizedTypeName.get(GUAVA_OPTIONAL, type)

  fun parameterizedJavaOptional(type: TypeName): TypeName =
      ParameterizedTypeName.get(JAVA_OPTIONAL, type)

  fun parameterizedInputType(type: TypeName): TypeName =
      ParameterizedTypeName.get(INPUT, type)

}