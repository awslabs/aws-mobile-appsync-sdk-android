/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.util.Log;

import com.amazonaws.apollographql.apollo.api.Operation;
import com.amazonaws.apollographql.apollo.cache.normalized.ApolloStore;
import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptor;
import com.amazonaws.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * AppSyncOptimisticUpdateInterceptor.
 */

class AppSyncOptimisticUpdateInterceptor implements ApolloInterceptor {
    private static final String TAG = AppSyncOptimisticUpdateInterceptor.class.getSimpleName();
    private ApolloStore store;

    public void setStore(ApolloStore store) {
        this.store = store;
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request,
                               @Nonnull ApolloInterceptorChain chain,
                               @Nonnull Executor dispatcher,
                               @Nonnull CallBack callBack) {
        if (request.optimisticUpdates.isPresent()) {
            final Operation.Data data = request.optimisticUpdates.get();
                dispatcher.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                           Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Updating store with the optimistic update for [" +  request.operation +"]");
                            store.write(request.operation, data).execute();
                        } catch (Exception e) {
                            Log.e(TAG, "Thread:[" + Thread.currentThread().getId() +"]: failed to update store with optimistic update for: [" + request.operation +"]");
                        }
                    }
                });
        }
        chain.proceedAsync(request, dispatcher, callBack);
    }

    @Override
    public void dispose() {
        // do nothing
    }
}
