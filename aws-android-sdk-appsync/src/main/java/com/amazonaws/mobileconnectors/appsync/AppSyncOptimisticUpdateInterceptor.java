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

package com.amazonaws.mobileconnectors.appsync;

import android.util.Log;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * AppSyncOptimisticUpdateInterceptor.
 */

class AppSyncOptimisticUpdateInterceptor implements ApolloInterceptor {

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
                            Log.d("AppSync", "Starting to do optimistic update!");
                            store.write(request.operation, data).execute();
                        } catch (Exception e) {
                            Log.e("AppSync", "failed to write operation optimistic updates, for: " + request.operation);
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
