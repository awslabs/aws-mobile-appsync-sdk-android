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
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    final AppSyncOfflineMutationInterceptor.QueueUpdateHandler queueHandler;
    boolean shouldRetry = true;
    Operation originalMutation;
    Operation currentMutation;
    String clientState;
    String recordIdentifier;
    AppSyncOfflineMutationManager appSyncOfflineMutationManager;

    private static final String TAG = InterceptorCallback.class.getSimpleName();

    public InterceptorCallback(ApolloInterceptor.CallBack customerCallBack,
                               AppSyncOfflineMutationInterceptor.QueueUpdateHandler handler,
                               final Operation originalMutation,
                               final  Operation currentMutation,
                               final String clientState,
                               final String recordIdentifier,
                               final AppSyncOfflineMutationManager appSyncOfflineMutationManager
    ) {
        this.customerCallBack = customerCallBack;
        this.queueHandler = handler;
        this.originalMutation = originalMutation;
        this.currentMutation = currentMutation;
        this.clientState = clientState;
        this.recordIdentifier = recordIdentifier;
        this.appSyncOfflineMutationManager = appSyncOfflineMutationManager;
    }

    @Override
    public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: onResponse()");

        //Check if the request failed due to a conflict
        if ((response.parsedResponse.get() != null) && (response.parsedResponse.get().hasErrors())) {
            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: onResponse -- found error");
            if ( response.parsedResponse.get().errors().get(0).toString().contains("The conditional request failed") ) {
                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: onResponse -- Got a string match in the errors for \"The conditional request failed\".");
                // if !shouldRetry AND conflict detected
                if (shouldRetry) {
                    Map data = (Map)((Error) response.parsedResponse.get().errors().get(0)).customAttributes().get("data");
                    //Verify that data was passed as per the contract from the server for mutation conflicts.
                    if (data != null ) {

                        //Found Conflict
                        String conflictString = new JSONObject(data).toString();
                        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Conflict String: " + conflictString);
                        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Client String: " + clientState);


                        //Send a message to the Queue handler to retry
                        Message message = new Message();
                        MutationInterceptorMessage msg = new MutationInterceptorMessage(originalMutation, currentMutation);
                        msg.serverState = conflictString;
                        msg.clientState = clientState;
                        msg.requestIdentifier = recordIdentifier;
                        msg.requestClassName = currentMutation.getClass().getSimpleName();
                        message.obj = msg;
                        message.what = MessageNumberUtil.RETRY_EXEC;
                        queueHandler.sendMessage(message);

                        //Set shouldRetry to false. Conflicts will only be attempted once.
                        shouldRetry = false;
                        return;
                    }
                }
            }
        }

        //Call the customer's callback
        customerCallBack.onResponse(response);

        //Set the mutation as completed.
        appSyncOfflineMutationManager.setInProgressMutationAsCompleted(recordIdentifier);

        //Send a message to the QueueHandler to process the next mutation in queue
        Message message = new Message();
        message.obj = new MutationInterceptorMessage(originalMutation, currentMutation);
        message.what = MessageNumberUtil.SUCCESSFUL_EXEC;
        queueHandler.sendMessage(message);
    }

    @Override
    public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: onFetch()");
        customerCallBack.onFetch(sourceType);
    }

    @Override
    public void onFailure(@Nonnull ApolloException e) {
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: onFailure() " + e.getLocalizedMessage());

        if (e instanceof ApolloNetworkException ) {
            //Happened due to a network error.
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Network Exception " + e.getLocalizedMessage());
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Will retry mutation when back on network");
            queueHandler.setMutationInProgressStatusToFalse();
            return;
        }

        shouldRetry = false;

        //Call the customer's callback
        customerCallBack.onFailure(e);

        //Set the mutation as completed.
        appSyncOfflineMutationManager.setInProgressMutationAsCompleted(recordIdentifier);

        //Send a message to the QueueHandler to process the next mutation in queue
        Message message = new Message();
        message.obj = new MutationInterceptorMessage(originalMutation, currentMutation);
        message.what = MessageNumberUtil.FAIL_EXEC;
        queueHandler.sendMessage(message);
        return;
    }

    @Override
    public void onCompleted() {
        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: onCompleted()");

    }
}

/**
 * AppSyncOfflineMutationInterceptor.
 */
class AppSyncOfflineMutationInterceptor implements ApolloInterceptor {

    final boolean sendOperationIdentifiers;
    final ScalarTypeAdapters scalarTypeAdapters;
    final AppSyncOfflineMutationManager appSyncOfflineMutationManager;
    Map<Mutation, MutationInformation> originalMutationRequestMap;
    AWSAppSyncClient appSyncClient;
    private QueueUpdateHandler queueHandler;
    private HandlerThread queueHandlerThread;
    final private ConflictResolverInterface conflictResolver;
    ConflictResolutionHandler conflictResolutionHandler;
    Map<String, ApolloInterceptor.CallBack> inmemoryInterceptorCallbackMap;
    Map<String, PersistentOfflineMutationObject> persistentOfflineMutationObjectMap;

    private static final String TAG = AppSyncOfflineMutationInterceptor.class.getSimpleName();

    class QueueUpdateHandler extends Handler {
        private final String TAG = QueueUpdateHandler.class.getSimpleName();

        //track when a mutation is in progress.
        private boolean mutationInProgress = false;

        //Mark the current mutation as complete.
        //This will be invoked on the onResults and onError flows of the mutation callback.
        public synchronized void setMutationInProgressStatusToFalse() {
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Setting mutationInProgress as false.");
            mutationInProgress = false;
        }

        //Return true if a mutation is currently in progress.
        public synchronized boolean isMutationInProgress() {
            return mutationInProgress;
        }


        //Attempt to mark the state to Mutation in progress and return true if successful.
        //Will return false otherwise.
        public synchronized boolean setMutationInProgress() {
            if (mutationInProgress ) {
                return false;
            }
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Setting mutationInProgress as true.");
            mutationInProgress = true;
            return true;
        }


        public QueueUpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        /*
            Kick of the next mutation in queue.
         */
        public void handleMessage(Message msg) {
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Got message to take action on the mutation queue.");

            if (msg.what == MessageNumberUtil.SUCCESSFUL_EXEC || msg.what == MessageNumberUtil.FAIL_EXEC) {
                if (!isMutationInProgress()) {
                    // start executing the next originalMutation
                    Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Got message to process next mutation if one exists.");
                    appSyncOfflineMutationManager.processNextInQueueMutation();
                }
            }
            else if (msg.what == MessageNumberUtil.RETRY_EXEC) {
                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Got message that a originalMutation process needs to be retried.");
                MutationInterceptorMessage interceptorMessage = (MutationInterceptorMessage)msg.obj;
                try {

                    conflictResolver.resolveConflict(conflictResolutionHandler,
                            new JSONObject(interceptorMessage.serverState),
                            new JSONObject(interceptorMessage.clientState),
                            interceptorMessage.requestIdentifier,
                            interceptorMessage.requestClassName);
                } catch (Exception e) {
                    Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: " + e.toString());
                    e.printStackTrace();
                }
            }
            else {
                // ignore case
                Log.d(TAG, "Unknown message received in QueueUpdateHandler. Ignoring");
            }
        }
    }



    public AppSyncOfflineMutationInterceptor(@Nonnull AppSyncOfflineMutationManager appSyncOfflineMutationManager,
                                             boolean sendOperationIdentifiers,
                                             Context context,
                                             Map<Mutation, MutationInformation> requestMap,
                                             AWSAppSyncClient client,
                                             ConflictResolverInterface conflictResolver) {

        final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
        this.scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);
        this.sendOperationIdentifiers = sendOperationIdentifiers;
        this.appSyncOfflineMutationManager = appSyncOfflineMutationManager;
        this.appSyncClient = client;
        this.originalMutationRequestMap = requestMap;

        queueHandlerThread = new HandlerThread("AWSAppSyncMutationQueueThread");
        queueHandlerThread.start();
        queueHandler = new QueueUpdateHandler(queueHandlerThread.getLooper());

        //Create a scheduled task that will run once every ten seconds to process mutations.
        //This is a catch all loop to provide a safety net for the mutation relay architecture.

        queueHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: processing Mutations");
                Message message = new Message();
                message.obj = new MutationInterceptorMessage();
                message.what = MessageNumberUtil.SUCCESSFUL_EXEC;
                queueHandler.sendMessage(message);
                queueHandler.postDelayed(this, 10* 1000);
            }
        }, 10* 1000);

        appSyncOfflineMutationManager.updateQueueHandler(queueHandler);
        inmemoryInterceptorCallbackMap = new HashMap<>();
        persistentOfflineMutationObjectMap = appSyncOfflineMutationManager.persistentOfflineMutationManager.persistentOfflineMutationObjectMap;

        conflictResolutionHandler =  new ConflictResolutionHandler(this);
        this.conflictResolver = conflictResolver;
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> void retryConflictMutation(@Nonnull Mutation<D, T, V> mutation, String uniqueIdintifierForOriginalMutation) {

        InterceptorCallback callback = (InterceptorCallback)inmemoryInterceptorCallbackMap.remove(uniqueIdintifierForOriginalMutation);

        Log.d("AppSync", "Callback is: "+ callback);
        // put in details of persistent mutation
        if (callback == null) {
            Log.d("AppSync", "Callback is null. great.");
            if (persistentOfflineMutationObjectMap.isEmpty()) {
                Log.d("AppSync", "Populating mutations map.");
                persistentOfflineMutationObjectMap.putAll(appSyncOfflineMutationManager.persistentOfflineMutationManager.persistentOfflineMutationObjectMap);
            }
            Log.d("AppSync", " + " + persistentOfflineMutationObjectMap.toString());
            PersistentOfflineMutationObject object = persistentOfflineMutationObjectMap.remove(uniqueIdintifierForOriginalMutation);
            persistentOfflineMutationObjectMap.put(mutation.toString(), object);
            Log.d("AppSync", " + " + persistentOfflineMutationObjectMap.toString());
        } else { // put in details of inmemory callback
            inmemoryInterceptorCallbackMap.put(mutation.toString(), callback);
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
        inmemoryInterceptorCallbackMap.remove(identifier);
        persistentOfflineMutationObjectMap.remove(identifier);

        queueHandler.setMutationInProgressStatusToFalse();
        queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request,
                               @Nonnull ApolloInterceptorChain chain,
                               @Nonnull Executor dispatcher,
                               @Nonnull final CallBack callBack) {

        //Check if this is a mutation request.
        if (!(request.operation instanceof  Mutation)) {
            //Not a mutation. Nothing to do here - move on to the next link in the chain.
            chain.proceedAsync(request, dispatcher, callBack);
            return;
        }

        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Processing mutation.");
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: First, checking if it is a retry of mutation that encountered a conflict.");

        //All mutations are added to the originalMutationRequestMap as part of processing. So if the mutation is present in the map,
        //it means that it has already been attempted once before.
        if (!originalMutationRequestMap.containsKey(request.operation)) {
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Not a conflict mutation");

            //Create a callback to inspect the results before calling caller provided callback.
            ApolloInterceptor.CallBack customCallBack = new InterceptorCallback(
                    callBack,
                    queueHandler,
                    (Mutation) request.operation,
                    (Mutation) request.operation,
                    new JSONObject(request.operation.variables().valueMap()).toString(),
                    request.uniqueId.toString(),
                    appSyncOfflineMutationManager);

            try {
                //Add the custom callback to the in memoryInterceptorCallback map
                inmemoryInterceptorCallbackMap.put(request.uniqueId.toString(), customCallBack);

                //Hand off to the appSyncOfflineMutationManager to do the work.
                appSyncOfflineMutationManager.addMutationObjectInQueue(new InMemoryOfflineMutationObject(request.uniqueId.toString(),
                        request,
                        chain,
                        dispatcher,
                        customCallBack));
            } catch (Exception e) {
                Log.e(TAG, "ERROR: "+ e);
                e.printStackTrace();
            }
        }

        else {
            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Yes, It is a conflict mutation. Processing the conflict with priority");
            originalMutationRequestMap.remove(request.operation.toString());
            InterceptorCallback originalCallback = (InterceptorCallback)inmemoryInterceptorCallbackMap.get(request.operation.toString());
            Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Original Callback which is going to be passed is: " + originalCallback);

            // this is due to the mutation being a persistent offline mutation which is being retried
            if (originalCallback == null) {
                final PersistentMutationsCallback persistentMutationsCallback =
                        appSyncOfflineMutationManager.persistentOfflineMutationManager.networkInvoker.persistentMutationsCallback;
                final PersistentOfflineMutationObject object = persistentOfflineMutationObjectMap.get(request.operation.toString());
                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Fetched object: " + object);

                chain.proceedAsync(request, dispatcher, new CallBack() {

                    @Override
                    public void onResponse(@Nonnull InterceptorResponse response) {
                        callBack.onResponse(response);
                        queueHandler.setMutationInProgressStatusToFalse();
                        queueHandler.sendEmptyMessage(MessageNumberUtil.SUCCESSFUL_EXEC);
                        if ( persistentMutationsCallback != null) {
                            JSONObject jsonObject;
                            try {
                                String response1 = response.clonedBufferString.get();
                                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: HTTP Response1: " + response1);
                                jsonObject = new JSONObject(response1);
                            } catch (Exception e) {
                                Log.e(TAG, "Thread:[" + Thread.currentThread().getId() +"]: " + e.getLocalizedMessage());
                                Log.e(TAG, "Thread:[" + Thread.currentThread().getId() +"]: " + e.getMessage());
                                e.printStackTrace();

                                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Looking to send error for: " + request.operation);
                                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: " + object);
                                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: " + persistentOfflineMutationObjectMap.toString());
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
                        queueHandler.setMutationInProgressStatusToFalse();
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

}
