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

import android.content.Context;
import android.util.Log;


import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.json.InputFieldJsonWriter;
import com.apollographql.apollo.internal.json.JsonWriter;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import okio.Buffer;

/**
 * InterceptorCallback.
 */
class InterceptorCallback implements ApolloInterceptor.CallBack {

    ApolloInterceptor.CallBack customerCallBack;

    public InterceptorCallback(ApolloInterceptor.CallBack customerCallBack) {
        this.customerCallBack = customerCallBack;
    }

    @Override
    public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        Log.d("AppSync", "onResponse()");
        customerCallBack.onResponse(response);
    }

    @Override
    public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {
        Log.d("AppSync", "onFetch()");
        customerCallBack.onFetch(sourceType);
    }

    @Override
    public void onFailure(@Nonnull ApolloException e) {
        Log.d("AppSync", "onFailure()" + e.getLocalizedMessage());
        customerCallBack.onFailure(e);
    }

    @Override
    public void onCompleted() {
        Log.d("AppSync", "onCompleted()");

    }
}

/**
 * AppSyncOfflineMutationInterceptor.
 */
class AppSyncOfflineMutationInterceptor implements ApolloInterceptor {

    CallBack customCallBack;
    CallBack customerCallBack;
    final boolean sendOperationIdentifiers;
    final ScalarTypeAdapters scalarTypeAdapters;
    final AppSyncOfflineMutationManager manager;
    Context mContext;

    public AppSyncOfflineMutationInterceptor(@Nonnull AppSyncOfflineMutationManager manager,
                                             boolean sendOperationIdentifiers,
                                             Context context) {

        final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
        this.scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);
        this.sendOperationIdentifiers = sendOperationIdentifiers;
        this.manager = manager;
        this.mContext = context;
    }

    @Override
    public void interceptAsync(@Nonnull InterceptorRequest request,
                               @Nonnull ApolloInterceptorChain chain,
                               @Nonnull Executor dispatcher,
                               @Nonnull CallBack callBack) {
        if (request.operation instanceof Query || isConnectionAvailable()) {
            chain.proceedAsync(request, dispatcher, callBack);
            return;
        }

        this.customerCallBack = callBack;
        customCallBack = new InterceptorCallback(this.customerCallBack);

        // handle offline mutation here..
        try {
            manager.addMutationObjectInQueue(new InMemoryOfflineMutationObject(request.operation.operationId(),
                    request,
                    chain,
                    dispatcher,
                    customCallBack));
        } catch (Exception e) {

        }

    }

    @Override
    public void dispose() {
        // do nothing
    }

    private boolean isConnectionAvailable() {
        return manager.shouldProcess();
    }

}
