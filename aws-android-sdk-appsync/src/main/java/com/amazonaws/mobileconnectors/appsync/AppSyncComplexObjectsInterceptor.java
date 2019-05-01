/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.S3InputObjectInterface;
import com.apollographql.apollo.api.S3ObjectManager;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.io.IOException;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * AppSyncComplexObjectsInterceptor.
 */

public class AppSyncComplexObjectsInterceptor implements ApolloInterceptor {

    final S3ObjectManager s3ObjectManager;
    private final static String TAG = AppSyncComplexObjectsInterceptor.class.getSimpleName();

    public AppSyncComplexObjectsInterceptor(S3ObjectManager s3ObjectManager) {
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Instantiating Complex Objects Interceptor");
        this.s3ObjectManager = s3ObjectManager;
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull final ApolloInterceptorChain chain, @Nonnull final Executor dispatcher, @Nonnull final CallBack callBack) {

        if (! (request.operation instanceof Mutation)) {
            chain.proceedAsync(request, dispatcher, callBack);
            return;
        }

        S3InputObjectInterface s3Object = S3ObjectManagerImplementation.getS3ComplexObject(request.operation.variables().valueMap());
        if (s3Object == null ) {
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: No s3 Objects found. Proceeding with the chain");
            chain.proceedAsync(request, dispatcher, callBack);
            return;
        }

        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Found S3Object. Performing upload");
        if (s3ObjectManager == null ) {
            callBack.onFailure(new ApolloException("S3 Object Manager not setup"));
            return;
        }

        try {
            s3ObjectManager.upload(s3Object);
        }
        catch(AmazonClientException e ) {
            if (e.getCause() instanceof IOException ) {
                Log.v(TAG, "Exception " + e);
                callBack.onFailure(new ApolloNetworkException("S3 upload failed.", e.getCause()));
                return;
            }
            callBack.onFailure(new ApolloException("S3 upload failed.", e.getCause()));
            return;
        }
        catch (Exception e ) {
            callBack.onFailure(new ApolloException("S3 upload failed.", e.getCause()));
            return;
        }

        chain.proceedAsync(request, dispatcher, callBack);
    }

    @Override
    public void dispose() {

    }
}
