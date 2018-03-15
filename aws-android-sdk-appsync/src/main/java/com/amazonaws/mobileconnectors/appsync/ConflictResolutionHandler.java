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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * ConflictResolutionHandler.
 */

public class ConflictResolutionHandler {

    final AppSyncOfflineMutationInterceptor mutationInterceptor;

    public ConflictResolutionHandler(@Nonnull AppSyncOfflineMutationInterceptor mutationInterceptor) {
        this.mutationInterceptor = mutationInterceptor;
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> void retryMutation(@Nonnull Mutation<D, T, V> mutation,
                                                                                         @Nonnull String identifier) {
        Log.d("AppSync", "Calling retry conflict mutation.");
        mutationInterceptor.retryConflictMutation(mutation, identifier);
    }

    public void fail(final String identifier) {
        mutationInterceptor.failConflictMutation(identifier);
    }
}
