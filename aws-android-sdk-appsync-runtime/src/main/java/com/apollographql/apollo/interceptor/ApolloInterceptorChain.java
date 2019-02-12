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

package com.apollographql.apollo.interceptor;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * ApolloInterceptorChain is responsible for building chain of {@link ApolloInterceptor} .
 */
public interface ApolloInterceptorChain {
  /**
   * Passes the control over to the next {@link ApolloInterceptor} in the responsibility chain and immediately exits as
   * this is a non blocking call. In order to receive the results back, pass in a callback which will handle the
   * received response or error.
   *
   * @param request    outgoing request object.
   * @param dispatcher the {@link Executor} which dispatches the calls asynchronously.
   * @param callBack   the callback which will handle the response or a failure exception.
   */
  void proceedAsync(@Nonnull ApolloInterceptor.InterceptorRequest request, @Nonnull Executor dispatcher,
                    @Nonnull ApolloInterceptor.CallBack callBack);

  /**
   * Disposes of the resources which are no longer required.
   */
  void dispose();
}
