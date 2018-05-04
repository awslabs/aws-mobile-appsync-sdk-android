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

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.interceptor.ApolloInterceptor;

enum MuationType {
    InMemory,
    Persistent
}

/**
 * MutationInformation.
 */
class MutationInformation {
    InMemoryOfflineMutationObject originalInMemoryMutation;
    PersistentOfflineMutationObject originalPersistMutation;
    String clientState;
    ApolloInterceptor.CallBack customerCallBack;
    Mutation retryMutation;
    MuationType muationType;
    String uniqueIdentifier;

    public MutationInformation(String uniqueIdentifier,
                               InMemoryOfflineMutationObject originalInMemoryMutation,
                               ApolloInterceptor.CallBack customerCallBack,
                               String clientState) {
        this.originalInMemoryMutation = originalInMemoryMutation;
        this.customerCallBack = customerCallBack;
        this.clientState = clientState;
        this.muationType = MuationType.InMemory;
        this.uniqueIdentifier = uniqueIdentifier;
    }

    public MutationInformation(String uniqueIdentifier,
                               PersistentOfflineMutationObject persistentOfflineMutationObject,
                               String clientState) {

        this.uniqueIdentifier = uniqueIdentifier;
        this.originalPersistMutation = persistentOfflineMutationObject;
        this.clientState = clientState;
        this.muationType = MuationType.Persistent;
    }

    void updateRetryMutation(Mutation retryMutation) {
        this.retryMutation = retryMutation;
    }

    void updateCustomerCallBack(ApolloInterceptor.CallBack customerCallBack) {
        this.customerCallBack = customerCallBack;
    }


}
