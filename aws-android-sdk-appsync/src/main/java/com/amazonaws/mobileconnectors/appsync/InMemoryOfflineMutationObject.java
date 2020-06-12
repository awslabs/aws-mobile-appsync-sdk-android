/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.util.Log;

import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptor;
import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * InMemoryOfflineMutationObject.
 */

class InMemoryOfflineMutationObject {

    final String recordIdentifier;
    final ApolloInterceptor.InterceptorRequest request;
    final ApolloInterceptorChain chain;
    final Executor dispatcher;
    final ApolloInterceptor.CallBack callBack;
    private static final String TAG = InMemoryOfflineMutationObject.class.getSimpleName();

    public InMemoryOfflineMutationObject(String recordIdentifier,
                                         @Nonnull ApolloInterceptor.InterceptorRequest request,
                                         @Nonnull ApolloInterceptorChain chain,
                                         @Nonnull Executor dispatcher,
                                         @Nonnull ApolloInterceptor.CallBack callBack) {
        this.recordIdentifier = recordIdentifier;
        this.request = request;
        this.chain = chain;
        this.dispatcher = dispatcher;
        this.callBack = callBack;
    }

    public void execute( ) {
        // execute the originalMutation by proceeding with the chain.
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Executing mutation by proceeding with the chain.");
        chain.proceedAsync(request, dispatcher, callBack);
    }
}
