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

import com.google.common.collect.Lists;

import com.moowork.gradle.node.task.NodeTask;

import org.gradle.api.internal.tasks.options.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApolloSchemaIntrospectionTask extends NodeTask{
  @Option(option = "url", description = "URL for the GraphQL server, also supports a local query file")
  private String url;
  @Option(option = "output", description = "Output path for the GraphQL schema file relative to the project root")
  private String output;
  @Option(option = "headers", description = "Additional Headers to send to the server")
  private List<String> headers;
  @Option(option = "insecure", description = "Allows \"insecure\" SSL connection to the server")
  private boolean insecure;

  public ApolloSchemaIntrospectionTask() {
    dependsOn(ApolloCodeGenInstallTask.NAME);
    headers = new ArrayList<>();
  }

  @Override
  public void exec() {
    if (Utils.isNullOrEmpty(url) || Utils.isNullOrEmpty(output)) {
      throw new IllegalArgumentException("Schema URL and output path can't be empty");
    }

    setScript(new File(getProject().getTasks().getByPath(ApolloCodeGenInstallTask.NAME).getOutputs().getFiles()
        .getAsPath(), ApolloIRGenTask.APOLLO_CODEGEN_EXEC_FILE));

    List<String> args = Lists.newArrayList("introspect-schema", url, "--output", getProject().file(output)
        .getAbsolutePath());

    if (!headers.isEmpty()) {
      for(String h : headers) {
        args.add("--header");
        args.add(h);
      }
    }

    if (insecure) {
      args.add("--insecure");
      args.add("true");
    }

    setArgs(args);
    super.exec();
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public void setHeaders(List<String> header) {
    this.headers = header;
  }

  public void setInsecure(boolean inSecure) {
    this.insecure = inSecure;
  }
}
