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

package com.apollographql.apollo.exception;

import javax.annotation.Nullable;

import okhttp3.Response;

public final class ApolloHttpException extends ApolloException {
  private final int code;
  private final String message;
  private final transient Response rawResponse;

  public ApolloHttpException(@Nullable Response rawResponse) {
    super(formatMessage(rawResponse));
    this.code = rawResponse != null ? rawResponse.code() : 0;
    this.message = rawResponse != null ? rawResponse.message() : "";
    this.rawResponse = rawResponse;
  }

  public int code() {
    return code;
  }

  public String message() {
    return message;
  }

  @Nullable public Response rawResponse() {
    return rawResponse;
  }

  private static String formatMessage(Response response) {
    if (response == null) {
      return "Empty HTTP response";
    }
    return "HTTP " + response.code() + " " + response.message();
  }
}
