/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.gradle;

import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import com.google.common.base.Joiner;

import com.apollographql.apollo.compiler.GraphQLCompiler;
import com.apollographql.apollo.compiler.NullableValueType;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileType;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public abstract class ApolloClassGenTask extends DefaultTask {
  static final String NAME = "generate%sApolloClasses";

  @Internal private String variant;
  @Internal private ApolloExtension apolloExtension;

  private PatternFilterable pattern = new PatternSet();

  private final ConfigurableFileCollection allSourceFiles = getProject().getObjects().fileCollection();

  @InputFiles
  @Incremental
  @PathSensitive(RELATIVE)
  private final ConfigurableFileCollection source = getProject().files().from(new Callable<FileTree>() {
    @Override
    public FileTree call() {
      return allSourceFiles.getAsFileTree().matching(pattern);
    }
  });

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
  void generateClasses(InputChanges inputs) {
    for (FileChange change : inputs.getFileChanges(getSource())) {
      if (change.getFileType() != FileType.FILE) {
        continue;
      }
      File inputFile = change.getFile();
      GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(inputFile, outputDir,
              apolloExtension.getCustomTypeMapping(), nullableValueType, apolloExtension.isGenerateAccessors(),
              apolloExtension.isUseSemanticNaming(), apolloExtension.isGenerateModelBuilder(),
              apolloExtension.getOutputPackageName());
      new GraphQLCompiler().write(args);
    }
  }

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }


  public FileCollection getSource() { return source; }

  public void setSource(Object source) {
    allSourceFiles.setFrom(source);
  }

  public void setInclude(String... include) {
    pattern.include(include);
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
