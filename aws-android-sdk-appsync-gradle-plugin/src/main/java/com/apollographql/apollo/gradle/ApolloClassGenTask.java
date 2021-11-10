/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.gradle;

import com.google.common.base.Joiner;

import com.apollographql.apollo.compiler.GraphQLCompiler;
import com.apollographql.apollo.compiler.NullableValueType;

import org.gradle.api.Action;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;

public class ApolloClassGenTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses";

  @Internal private String variant;
  @Internal private ApolloExtension apolloExtension;
  @OutputDirectory private File outputDir;
  @Internal private NullableValueType nullableValueType;

  public void init(String variant, ApolloExtension apolloExtension) {
    this.variant = variant;
    this.apolloExtension = apolloExtension;
    nullableValueType = apolloExtension.getNullableValueType() == null
        ? NullableValueType.ANNOTATED
        : NullableValueType.Companion.findByValue(apolloExtension.getNullableValueType());
    outputDir = new File(getProject().getBuildDir() + File.separator + Joiner.on(File.separator).join(GraphQLCompiler.Companion
        .getOUTPUT_DIRECTORY()));
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      public void execute(InputFileDetails inputFileDetails) {
        File inputFile = inputFileDetails.getFile();
        if (!inputFile.isFile()) {
          // skip if input is not a file
          return;
        }
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(inputFile, outputDir,
            apolloExtension.getCustomTypeMapping(), nullableValueType, apolloExtension.isGenerateAccessors(),
            apolloExtension.isUseSemanticNaming(), apolloExtension.isGenerateModelBuilder(),
            apolloExtension.getOutputPackageName());
        new GraphQLCompiler().write(args);
      }
    });
  }

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public File getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  public NullableValueType getNullableValueType() {
    return nullableValueType;
  }

  public void setNullableValueType(NullableValueType nullableValueType) {
    this.nullableValueType = nullableValueType;
  }

  public ApolloExtension getApolloExtension() {
    return apolloExtension;
  }

  public void setApolloExtension(ApolloExtension apolloExtension) {
    this.apolloExtension = apolloExtension;
  }
}
