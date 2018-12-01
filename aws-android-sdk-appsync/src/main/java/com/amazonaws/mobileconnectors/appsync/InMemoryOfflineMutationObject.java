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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import static com.amazonaws.mobileconnectors.appsync.AppSyncOfflineMutationManager.MSG_EXEC;

/**
 * InMemoryOfflineMutationObject.
 */

class InMemoryOfflineMutationObject {

    final String recordIdentifier;
    final Handler handler;
    final ApolloInterceptor.InterceptorRequest request;
    final ApolloInterceptorChain chain;
    final Executor dispatcher;
    final ApolloInterceptor.CallBack callBack;
    private HandlerThread queueHandlerThread;
    private Handler queueHandler;
    private static final String TAG = InMemoryOfflineMutationObject.class.getSimpleName();

    public InMemoryOfflineMutationObject(String recordIdentifier,
                                         @Nonnull ApolloInterceptor.InterceptorRequest request,
                                         @Nonnull ApolloInterceptorChain chain,
                                         @Nonnull Executor dispatcher,
                                         @Nonnull ApolloInterceptor.CallBack callBack) {
        this.recordIdentifier = recordIdentifier;
        this.handler = new NetworkUpdateHandler();
        this.request = request;
        this.chain = chain;
        this.dispatcher = dispatcher;
        this.callBack = callBack;
    }

    class NetworkUpdateHandler extends Handler {
        public NetworkUpdateHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_EXEC) {
                // start executing the originalMutation
                Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Executing mutation by proceeding with the chain.");
                chain.proceedAsync(request, dispatcher, callBack);
            }
            else {
                // ignore case
                Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Non MSG_EXEC message received in NetworkUpdateHandler. Ignoring...");
            }
        }
    }

}
