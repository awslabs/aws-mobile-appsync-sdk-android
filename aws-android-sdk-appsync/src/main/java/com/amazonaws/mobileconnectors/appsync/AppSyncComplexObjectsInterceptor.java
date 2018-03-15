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

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.S3InputObjectInterface;
import com.apollographql.apollo.api.S3ObjectManager;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * AppSyncComplexObjectsInterceptor.
 */

public class AppSyncComplexObjectsInterceptor implements ApolloInterceptor {

    final S3ObjectManager s3ObjectManager;

    public AppSyncComplexObjectsInterceptor(S3ObjectManager s3ObjectManager) {
        this.s3ObjectManager = s3ObjectManager;
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull final ApolloInterceptorChain chain, @Nonnull final Executor dispatcher, @Nonnull final CallBack callBack) {
        if (request.operation instanceof Mutation
                && request.operation.queryDocument().contains("S3ObjectInput")) {
            Log.d("AppSync", "Found S3ObjectInput data type, finding object to upload if any.");
            dispatcher.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        performS3ComplexObjectUpload(request.operation);
                    } catch (Exception e) {
                        callBack.onFailure(new ApolloNetworkException("S3 upload failed.", e));
                        callBack.onCompleted();
                    }
                    chain.proceedAsync(request, dispatcher, callBack);
                }
            });

        } else {
            chain.proceedAsync(request, dispatcher, callBack);
        }
    }

    private void performS3ComplexObjectUpload(Operation operation) throws Exception {
        S3InputObjectInterface s3InputObject = getS3ComplexObject(operation.variables().valueMap());
        if (s3InputObject != null) {
            s3ObjectManager.upload(s3InputObject);
        }
    }

    private S3InputObjectInterface getS3ComplexObject(Map<String, Object> variablesMap) {
        for (String key: variablesMap.keySet()) {
            if (variablesMap.get(key) instanceof S3InputObjectInterface) {
                S3InputObjectInterface s3InputObject = (S3InputObjectInterface)variablesMap.get(key);
                return s3InputObject;
            } else {
                if (variablesMap.get(key) instanceof Map) {
                    return getS3ComplexObject((Map<String, Object>) variablesMap.get(key));
                }
            }
        }
        return null;
    }

    @Override
    public void dispose() {

    }
}
