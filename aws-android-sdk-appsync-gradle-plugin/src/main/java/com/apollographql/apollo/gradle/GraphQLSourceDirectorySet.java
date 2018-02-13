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

package com.apollographql.apollo.gradle;

import com.apollographql.apollo.compiler.GraphQLCompiler;

import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;

public class GraphQLSourceDirectorySet extends DefaultSourceDirectorySet {
  static final String SCHEMA_FILE_NAME = "schema.json";
  static final String NAME = "graphql";

  private static final String GRAPHQL_QUERY_PATTERN = "**/*." + GraphQLCompiler.FILE_EXTENSION;
  private static final String SCHEMA_FILE_PATTERN = "**/" + SCHEMA_FILE_NAME;

  public GraphQLSourceDirectorySet(String name, FileResolver fileResolver) {
      super(name, String.format("%s GraphQL source", name), fileResolver, new DefaultDirectoryFileTreeFactory());
      srcDir("src/" + name + "/graphql");
      include(GRAPHQL_QUERY_PATTERN, SCHEMA_FILE_PATTERN);
  }
}
