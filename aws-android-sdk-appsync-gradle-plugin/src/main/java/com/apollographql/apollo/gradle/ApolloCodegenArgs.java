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
