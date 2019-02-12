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

package com.apollographql.apollo.compiler.ir

sealed class ScalarType(val name: String) {
  object ID : ScalarType("ID")
  object STRING : ScalarType("String")
  object INT : ScalarType("Int")
  object BOOLEAN : ScalarType("Boolean")
  object FLOAT : ScalarType("Float")
  object AWS_TIMESTAMP : ScalarType("AWSTimestamp")
}