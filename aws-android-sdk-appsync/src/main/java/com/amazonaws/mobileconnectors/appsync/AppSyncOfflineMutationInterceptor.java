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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

class MutationInterceptorMessage {
    final Operation originalMutation;
    final Operation currentMutation;
    String requestIdentifier;
    String requestClassName;
    String serverState;
    String clientState;

    MutationInterceptorMessage(Operation originalMutation, Operation currentMutation) {
        this.originalMutation = originalMutation;
        this.currentMutation = currentMutation;
    }
    MutationInterceptorMessage() {
        this.originalMutation = null;
        this.currentMutation = null;
    }
}

/**
 * InterceptorCallback.
 */
class InterceptorCallback implements ApolloInterceptor.CallBack {

    ApolloInterceptor.CallBack customerCallBack;
    final Handler handler;
    boolean shouldRetry = true;
    Operation originalMutation;
    Operation currentMutation;
    String clientState;
    String recordIdentifier;
    private static final String TAG = InterceptorCallback.class.getSimpleName();

    public InterceptorCallback(ApolloInterceptor.CallBack customerCallBack, Handler handler,
                               final Operation originalMutation,
                               final  Operation currentMutation,
                               final String clientState,
                               final String recordIdentifier) {
        this.customerCallBack = customerCallBack;
        this.handler = handler;
        this.originalMutation = originalMutation;
        this.currentMutation = currentMutation;
        this.clientState = clientState;
        this.recordIdentifier = recordIdentifier;

    }

    @Override
    public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        Log.d(TAG, "onResponse()");
        //The conditional request failed
        if ((response.parsedResponse.get() != null) && (response.parsedResponse.get().hasErrors())) {
            Log.d(TAG, "onResponse -- found error");
            if ( response.parsedResponse.get().errors().get(0).toString().contains("The conditional request failed") ) {
                Log.d(TAG, "onResponse -- Got a string match in the errors for \"The conditional request failed\".");
                // if !shouldRetry AND conflict detected
                if (shouldRetry) {
                    Map data = (Map)((Error) response.parsedResponse.get().errors().get(0)).customAttributes().get("data");
                    //Verify that data was passed as per the contract from the server for mutation conflicts.
                    if (data != null ) {
                        String conflictString = new JSONObject(data).toString();
                        Log.d(TAG, "Conflict String: " + conflictString);
                        Log.d(TAG, "Client String: " + clientState);
                        Message message = new Message();
                        MutationInterceptorMessage msg = new MutationInterceptorMessage(originalMutation, currentMutation);
                        msg.serverState = conflictString;
                        msg.clientState = clientState;
                        msg.requestIdentifier = recordIdentifier;
                        msg.requestClassName = currentMutation.getClass().getSimpleName();
                        //msg.requestIdentifier = originalMutation.un
                        message.obj = msg;
                        message.what = MessageNumberUtil.RETRY_EXEC;
                        handler.sendMessage(message);
                        shouldRetry = false;
                        return;
                    }
                }
            }
        }

        customerCallBack.onResponse(response);
        Message message = new Message();
        message.obj = new MutationInterceptorMessage(originalMutation, currentMutation);
        message.what = MessageNumberUtil.SUCCESSFUL_EXEC;
        handler.sendMessage(message);
    }

    @Override
    public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {
        Log.d("AppSync", "onFetch()");
        customerCallBack.onFetch(sourceType);
    }

    @Override
    public void onFailure(@Nonnull ApolloException e) {
        Log.d("AppSync", "onFailure()" + e.getLocalizedMessage());
        shouldRetry = false;
        Message message = new Message();
        message.obj = new MutationInterceptorMessage(originalMutation, currentMutation);
        message.what = MessageNumberUtil.FAIL_EXEC;
        handler.sendMessage(message);
        customerCallBack.onFailure(e);
        return;
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

    final boolean sendOperationIdentifiers;
    final ScalarTypeAdapters scalarTypeAdapters;
    final AppSyncOfflineMutationManager manager;
    Map<Mutation, MutationInformation> originalMutationRequestMap;
    AWSAppSyncClient appSyncClient;
    private QueueUpdateHandler queueHandler;
    private HandlerThread queueHandlerThread;
    final private ConflictResolverInterface conflictResolver;
    ConflictResolutionHandler conflictResolutionHandler;
    Map<String, ApolloInterceptor.CallBack> inmemoryInterceptorCallback;
    Map<String, PersistentOfflineMutationObject> persistentOfflineMutationObjectMap;

    class QueueUpdateHandler extends Handler {

        public QueueUpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d("AppSync", "Got message to take action regarding mutation queue.");
            if (msg.what == MessageNumberUtil.SUCCESSFUL_EXEC) {
                // start executing the next originalMutation
                Log.d("AppSync", "Got message to process next mutations if any.");
                manager.processNextInQueueMutation();
            } else if (msg.what == MessageNumberUtil.FAIL_EXEC) {
                Log.d("AppSync", "Got message that a originalMutation process errored out.");
                manager.processNextInQueueMutation();
            } else if (msg.what == MessageNumberUtil.RETRY_EXEC) {
                Log.d("AppSync", "Got message that a originalMutation process needs to be retried.");
                MutationInterceptorMessage interceptorMessage = (MutationInterceptorMessage)msg.obj;
                try {

                    conflictResolver.resolveConflict(conflictResolutionHandler,
                            new JSONObject(interceptorMessage.serverState),
                            new JSONObject(interceptorMessage.clientState),
                            interceptorMessage.requestIdentifier,
                            interceptorMessage.requestClassName);
                } catch (Exception e) {
                    Log.d("AppSync", e.toString());
                    e.printStackTrace();
                }
            }  else {
                // ignore case
                Log.d("AppSync", "Unknown message received in QueueUpdateHandler.");
            }
        }
    }

    public AppSyncOfflineMutationInterceptor(@Nonnull AppSyncOfflineMutationManager manager,
                                             boolean sendOperationIdentifiers,
                                             Context context,
                                             Map<Mutation, MutationInformation> requestMap,
                                             AWSAppSyncClient client,
                                             ConflictResolverInterface conflictResolver) {

        final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
        this.scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);
        this.sendOperationIdentifiers = sendOperationIdentifiers;
        this.manager = manager;
        this.appSyncClient = client;
        this.originalMutationRequestMap = requestMap;
        queueHandlerThread = new HandlerThread("AWSAppSyncMutationQueueThread");
        queueHandlerThread.start();
        queueHandler = new QueueUpdateHandler(queueHandlerThread.getLooper());
        manager.updateQueueHandler(queueHandler);
        inmemoryInterceptorCallback = new HashMap<>();
        persistentOfflineMutationObjectMap = manager.persistentOfflineMutationManager.persistentOfflineMutationObjectMap;

        conflictResolutionHandler =  new ConflictResolutionHandler(this);
        this.conflictResolver = conflictResolver;
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> void retryConflictMutation(@Nonnull Mutation<D, T, V> mutation, String uniqueIdintifierForOriginalMutation) {

        InterceptorCallback callback = (InterceptorCallback)inmemoryInterceptorCallback.remove(uniqueIdintifierForOriginalMutation);

        Log.d("AppSync", "Callback is: "+ callback);
        // put in details of persistent mutation
        if (callback == null) {
            Log.d("AppSync", "Callback is null. great.");
            if (persistentOfflineMutationObjectMap.isEmpty()) {
                Log.d("AppSync", "Populating mutations map.");
                persistentOfflineMutationObjectMap.putAll(manager.persistentOfflineMutationManager.persistentOfflineMutationObjectMap);
            }
            Log.d("AppSync", " + " + persistentOfflineMutationObjectMap.toString());
            PersistentOfflineMutationObject object = persistentOfflineMutationObjectMap.remove(uniqueIdintifierForOriginalMutation);
            persistentOfflineMutationObjectMap.put(mutation.toString(), object);
            Log.d("AppSync", " + " + persistentOfflineMutationObjectMap.toString());
        } else { // put in details of inmemory callback
            inmemoryInterceptorCallback.put(mutation.toString(), callback);
        }

        Log.d("AppSync", "Now making the mutate call using client: " + appSyncClient);
        // GraphQLCall call = (GraphQLCall) ((InterceptorCallback)originalMutationRequestMap.get(currentMutationIdentifier).callBack).customerCallBack;
        appSyncClient.mutate(mutation, true).enqueue(new GraphQLCall.Callback<T>() {
            @Override
            public void onResponse(@Nonnull Response<T> response) {

            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {

            }
        });
    }

    public void failConflictMutation(String identifier) {
        originalMutationRequestMap.remove(identifier);
        inmemoryInterceptorCallback.remove(identifier);
        persistentOfflineMutationObjectMap.remove(identifier);
        manager.processNextInQueueMutation();
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request,
                               @Nonnull ApolloInterceptorChain chain,
                               @Nonnull Executor dispatcher,
                               @Nonnull final CallBack callBack) {
        final boolean isMutation = request.operation instanceof Mutation;

        if (!isMutation) {
            chain.proceedAsync(request, dispatcher, callBack);
            return;
        }
        Log.d("AppSync", "Checking if it is conflict mutation.");

        if (!originalMutationRequestMap.containsKey(request.operation)) {

            ApolloInterceptor.CallBack customCallBack = new InterceptorCallback(callBack,
                    queueHandler,
                    (Mutation) request.operation,
                    (Mutation) request.operation,
                    new JSONObject(request.operation.variables().valueMap()).toString(),
                    request.uniqueId.toString());

            // handle any non-conflicted mutation here.
            try {
                inmemoryInterceptorCallback.put(request.uniqueId.toString(), customCallBack);
                manager.addMutationObjectInQueue(new InMemoryOfflineMutationObject(request.uniqueId.toString(),
                        request,
                        chain,
                        dispatcher,
                        customCallBack));
            } catch (Exception e) {
                Log.e("AppSync", "ERROR: "+ e);
                e.printStackTrace();
            }
        } else {
            Log.d("AppSync", "Processing a conflict with priority");
            originalMutationRequestMap.remove(request.operation.toString());
            InterceptorCallback originalCallback = (InterceptorCallback)inmemoryInterceptorCallback.get(request.operation.toString());
            Log.d("AppSync", "Original Callback which is going to be passed is: " + originalCallback);

            // this is due to the mutation being a persistent offline mutation which is being retried
            if (originalCallback == null) {
                final PersistentMutationsCallback persistentMutationsCallback =
                        manager.persistentOfflineMutationManager.networkInvoker.persistentMutationsCallback;
                final PersistentOfflineMutationObject object = persistentOfflineMutationObjectMap.get(request.operation.toString());
                Log.d("AppSync", "Fetched object: " + object);

                chain.proceedAsync(request, dispatcher, new CallBack() {

                    @Override
                    public void onResponse(@Nonnull InterceptorResponse response) {
                        callBack.onResponse(response);
                        queueHandler.sendEmptyMessage(MessageNumberUtil.SUCCESSFUL_EXEC);
                        if ( persistentMutationsCallback != null) {
                            JSONObject jsonObject;
                            try {
                                String response1 = response.clonedBufferString.get();
                                Log.d("AppSync", "HTTP Response1: " + response1);
                                jsonObject = new JSONObject(response1);
                            } catch (Exception e) {
                                Log.e("AppSync", e.getLocalizedMessage());
                                Log.e("AppSync", e.getMessage());
                                e.printStackTrace();

                                Log.d("AppSync", "Looking to send error for: " + request.operation);
                                Log.d("AppSync", " " + object);
                                Log.d("AppSync", " " + persistentOfflineMutationObjectMap.toString());
                                persistentMutationsCallback.onFailure(new PersistentMutationsError(
                                        request.operation.getClass().getSimpleName(),
                                        object.recordIdentifier,
                                        new ApolloParseException(e.getLocalizedMessage()))
                                );
                                return;
                            }

                            JSONObject data = null;
                            try {
                                data = jsonObject.getJSONObject("data");
                            } catch (JSONException e) {
                                // do nothing as it indicates there were errors returned by server an data was null
                            }
                            JSONArray errors = null;
                            try {
                            if (data == null) {
                                errors = jsonObject.getJSONArray("errors");
                            }
                            } catch (JSONException e) {
                                persistentMutationsCallback.onFailure(new PersistentMutationsError(
                                        request.operation.getClass().getSimpleName(),
                                        object.recordIdentifier,
                                        new ApolloParseException(e.getLocalizedMessage()))
                                );
                                return;
                            }
                            if(persistentMutationsCallback != null) {
                                persistentMutationsCallback.onResponse(new PersistentMutationsResponse(
                                        data,
                                        errors,
                                        request.operation.getClass().getSimpleName(),
                                        object.recordIdentifier));
                            }
                        }
                    }

                    @Override
                    public void onFetch(FetchSourceType sourceType) {
                        callBack.onFetch(sourceType);
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        callBack.onFailure(e);
                        queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                        if ( persistentMutationsCallback != null) {
                            persistentMutationsCallback.onFailure(
                                    new PersistentMutationsError(request.operation.getClass().getSimpleName(),
                                            object.recordIdentifier,
                                            e));
                        }

                    }

                    @Override
                    public void onCompleted() {

                    }
                });
                return;
            }

            // proceed normally for in-memory callbacks
            chain.proceedAsync(request,dispatcher,originalCallback);
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
