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
  private NullableValueType nullableValueType;

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
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(inputFileDetails.getFile(), outputDir,
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
