/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
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
