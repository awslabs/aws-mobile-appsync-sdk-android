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

package com.apollographql.apollo.fetcher;

import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.internal.ApolloLogger;

/**
 * A ResponseFetcher is an {@link ApolloInterceptor} inserted at the beginning of a request chain.
 * It can control how a request is fetched by configuring {@link com.apollographql.apollo.interceptor.FetchOptions}.
 *
 * See {@link AppSyncResponseFetchers} for a basic set of fetchers.
 */
public interface ResponseFetcher {

  /**
   * @param logger A {@link ApolloLogger} to log relevant fetch information.
   * @return The {@link ApolloInterceptor} that executes the fetch logic.
   */
  ApolloInterceptor provideInterceptor(ApolloLogger logger);

}
