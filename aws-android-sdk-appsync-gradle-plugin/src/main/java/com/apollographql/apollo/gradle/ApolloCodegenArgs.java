/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.gradle;

import java.io.File;
import java.util.Set;

final class ApolloCodegenArgs {
  final File schemaFile;
  final Set<String> queryFilePaths;
  final File irOutputFolder;

  ApolloCodegenArgs(File schemaFile, Set<String> queryFilePaths, File irOutputFolder) {
    this.schemaFile = schemaFile;
    this.queryFilePaths = queryFilePaths;
    this.irOutputFolder = irOutputFolder;
  }
}
