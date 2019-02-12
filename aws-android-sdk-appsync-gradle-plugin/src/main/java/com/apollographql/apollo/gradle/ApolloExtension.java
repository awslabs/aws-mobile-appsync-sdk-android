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

import com.apollographql.apollo.compiler.NullableValueType;

import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApolloExtension {
  static final String NAME = "apollo";
  private Map<String, String> customTypeMapping = new LinkedHashMap<>();
  private String nullableValueType = NullableValueType.ANNOTATED.getValue();
  private boolean generateAccessors = true;
  private boolean useSemanticNaming = true;
  private boolean generateModelBuilder;
  private String schemaFilePath;
  private String outputPackageName;

  public Map<String, String> getCustomTypeMapping() {
    return customTypeMapping;
  }

  public void setCustomTypeMapping(Map<String, String> customTypeMapping) {
    this.customTypeMapping = customTypeMapping;
  }

  public String getNullableValueType() {
    return nullableValueType;
  }

  public void setNullableValueType(String nullableValueType) {
    this.nullableValueType = nullableValueType;
  }

  public void setUseSemanticNaming(boolean useSemanticNaming) {
    this.useSemanticNaming = useSemanticNaming;
  }

  public boolean isUseSemanticNaming() {
    return useSemanticNaming;
  }

  public boolean isGenerateAccessors() {
    return generateAccessors;
  }

  public void setGenerateAccessors(boolean generateAccessors) {
    this.generateAccessors = generateAccessors;
  }

  public boolean isGenerateModelBuilder() {
    return generateModelBuilder;
  }

  public void setGenerateModelBuilder(boolean generateModelBuilder) {
    this.generateModelBuilder = generateModelBuilder;
  }

  public void setCustomTypeMapping(Closure closure) {
    closure.setDelegate(customTypeMapping);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
  }

  public String getSchemaFilePath() {
    return schemaFilePath;
  }

  public void setSchemaFilePath(String schemaFilePath) {
    this.schemaFilePath = schemaFilePath;
  }

  public String getOutputPackageName() {
    return outputPackageName;
  }

  public void setOutputPackageName(String outputPackageName) {
    this.outputPackageName = outputPackageName;
  }
}
