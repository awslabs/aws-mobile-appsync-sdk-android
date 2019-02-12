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

package com.amazonaws.mobileconnectors.appsync;

import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * AppSyncMutationQueueInterceptor.
 */

public class AppSyncMutationQueueInterceptor implements ApolloInterceptor {

    Map<String, ConflictMutation> mutationMap;

    public AppSyncMutationQueueInterceptor(Map<String, ConflictMutation> mutationMap) {
        this.mutationMap = mutationMap;
    }

    @Override
    public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain, @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        if(mutationMap.containsKey(request.operation.operationId())) {
            // already in map
        }
    }

    @Override
    public void dispose() {

    }
}
