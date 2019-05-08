/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
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
