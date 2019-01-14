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

        if(shouldRetry && ConflictResolutionHandler.conflictPresent(response.parsedResponse)) {
            //Set shouldRetry to false. Conflicts will only be attempted once.
            shouldRetry = false;

            //Found Conflict
            String conflictString = new JSONObject((Map)((Error) response.parsedResponse.get().errors().get(0)).customAttributes().get("data")).toString();

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
            return;
        }

        //Call the customer's callback
        customerCallBack.onResponse(response);

        //Set the mutation as completed.
        appSyncOfflineMutationManager.setInProgressMutationAsCompleted(recordIdentifier);

        //Send a message to the QueueHandler to process the next mutation in queue
        Message message = new Message();
        message.obj = new MutationInterceptorMessage();
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
    private Map<Mutation, MutationInformation> mutationsToRetryAfterConflictResolution;
    AWSAppSyncClient appSyncClient;
    private QueueUpdateHandler queueHandler;
    private HandlerThread queueHandlerThread;
    final private ConflictResolverInterface conflictResolver;
    ConflictResolutionHandler conflictResolutionHandler;
    Map<String, ApolloInterceptor.CallBack> callbackMapForInMemoryMutations;
    Map<String, PersistentOfflineMutationObject> persistentOfflineMutationObjectMap;

    private static final String TAG = AppSyncOfflineMutationInterceptor.class.getSimpleName();
    private static final long QUEUE_POLL_INTERVAL = 10* 1000;

    class QueueUpdateHandler extends Handler {
        private final String TAG = QueueUpdateHandler.class.getSimpleName();

        //track when a mutation is in progress.
        private boolean mutationInProgress = false;


        //Mark the current mutation as complete.
        //This will be invoked on the onResults and onError flows of the mutation callback.
        synchronized void setMutationInProgressStatusToFalse() {
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Setting mutationInProgress as false.");
            mutationInProgress = false;
        }

        //Return true if a mutation is currently in progress.
        synchronized boolean isMutationInProgress() {
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
                    // start executing the next Mutation
                    Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Got message to process next mutation if one exists.");
                    appSyncOfflineMutationManager.processNextInQueueMutation();
                }
            }
            else if (msg.what == MessageNumberUtil.RETRY_EXEC) {
                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Got message that a originalMutation process needs to be retried.");
                MutationInterceptorMessage interceptorMessage = (MutationInterceptorMessage)msg.obj;
                try {
                    if (conflictResolver != null ) {
                        conflictResolver.resolveConflict(conflictResolutionHandler,
                                new JSONObject(interceptorMessage.serverState),
                                new JSONObject(interceptorMessage.clientState),
                                interceptorMessage.requestIdentifier,
                                interceptorMessage.requestClassName);
                    }
                    else {
                        failConflictMutation(interceptorMessage.requestIdentifier);
                    }
                } catch (Exception e) {
                    Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: " + e.toString());
                    e.printStackTrace();
                }
            }
            else {
                // ignore case
                Log.d(TAG, "Unknown message received in QueueUpdateHandler. Ignoring");
            }
            //Execute failsafe to make sure there isn't a stuck mutation in the queue
            checkAndHandleStuckMutation();
        }

        //Default queueTimeout for Mutations
        private long maxMutationExecutionTime;
        //Grace period for cancelled mutations
        private final long CANCEL_WINDOW = QUEUE_POLL_INTERVAL + (5 *1000);

        //Tracking in process mutation using these instance variables
        private InMemoryOfflineMutationObject inMemoryOfflineMutationObjectBeingExecuted = null;
        private PersistentOfflineMutationObject persistentOfflineMutationObjectBeingExecuted = null;

        //Start time for mutation
        private long startTime = 0;

        void setMaximumMutationExecutionTime(long time) {
            maxMutationExecutionTime = time;
        }
        void setInMemoryOfflineMutationObjectBeingExecuted(InMemoryOfflineMutationObject m) {
            inMemoryOfflineMutationObjectBeingExecuted = m;
            startTime = System.currentTimeMillis();
        }

        void setPersistentOfflineMutationObjectBeingExecuted(PersistentOfflineMutationObject p ) {
            persistentOfflineMutationObjectBeingExecuted = p;
            startTime = System.currentTimeMillis();
        }

        void clearPersistentOfflineMutationObjectBeingExecuted() {
            persistentOfflineMutationObjectBeingExecuted = null;
            startTime = 0;
        }

        void clearInMemoryOfflineMutationObjectBeingExecuted() {
            inMemoryOfflineMutationObjectBeingExecuted = null;
            startTime = 0;
        }

        private void checkAndHandleStuckMutation() {
            //Return if there is currently no mutation is in progress.
            if ( inMemoryOfflineMutationObjectBeingExecuted == null && persistentOfflineMutationObjectBeingExecuted == null ) {
                return;
            }

            //Calculate elapsed time
            long elapsedTime = System.currentTimeMillis() - startTime;

            //Handle persistentOfflineMutationObject
            if ( persistentOfflineMutationObjectBeingExecuted != null ) {

                //If time has elapsed past the cancel window, set this mutation as done and signal queueHandler to move
                //to the next in queue.
                if ( elapsedTime > (maxMutationExecutionTime + CANCEL_WINDOW)) {
                    appSyncOfflineMutationManager.setInProgressMutationAsCompleted(persistentOfflineMutationObjectBeingExecuted.recordIdentifier);
                    sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                }
                //If time has elapsed past the queueTimeout, mark the mutation as timed out.
                else if (elapsedTime > maxMutationExecutionTime) {
                    //Signal to persistentOfflineMutationManager that this muation has been timed out.
                    appSyncOfflineMutationManager.persistentOfflineMutationManager.addTimedoutMutation(persistentOfflineMutationObjectBeingExecuted);
                    appSyncOfflineMutationManager.persistentOfflineMutationManager.removePersistentMutationObject(persistentOfflineMutationObjectBeingExecuted.recordIdentifier);
                }
                return;
            }

            //Handle inMemory Mutation Object
            if (elapsedTime > (maxMutationExecutionTime + CANCEL_WINDOW)) {
                //If time has elapsed past the cancel window, set this mutation as done and signal queueHandler to move
                //to the next in queue.
                appSyncOfflineMutationManager.setInProgressMutationAsCompleted(inMemoryOfflineMutationObjectBeingExecuted.recordIdentifier);
                sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
            }
            else if ( elapsedTime > maxMutationExecutionTime) {
                //If time has elapsed past the queueTimeout, cancel the mutation by invoking dispose on the chain.
                inMemoryOfflineMutationObjectBeingExecuted.chain.dispose();
                dispose((Mutation) inMemoryOfflineMutationObjectBeingExecuted.request.operation);
            }
        }
    }



    public AppSyncOfflineMutationInterceptor(@Nonnull AppSyncOfflineMutationManager appSyncOfflineMutationManager,
                                             boolean sendOperationIdentifiers,
                                             Context context,
                                             Map<Mutation, MutationInformation> requestMap,
                                             AWSAppSyncClient client,
                                             ConflictResolverInterface conflictResolver,
                                             long maxMutationExecutionTime) {

        final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
        this.scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);
        this.sendOperationIdentifiers = sendOperationIdentifiers;
        this.appSyncOfflineMutationManager = appSyncOfflineMutationManager;
        this.appSyncClient = client;
        this.mutationsToRetryAfterConflictResolution = requestMap;

        queueHandlerThread = new HandlerThread("AWSAppSyncMutationQueueThread");
        queueHandlerThread.start();
        queueHandler = new QueueUpdateHandler(queueHandlerThread.getLooper());
        queueHandler.setMaximumMutationExecutionTime(maxMutationExecutionTime);

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
                queueHandler.postDelayed(this, QUEUE_POLL_INTERVAL);
            }
        }, QUEUE_POLL_INTERVAL);

        appSyncOfflineMutationManager.updateQueueHandler(queueHandler);
        callbackMapForInMemoryMutations = new HashMap<>();
        persistentOfflineMutationObjectMap = appSyncOfflineMutationManager.persistentOfflineMutationManager.persistentOfflineMutationObjectMap;

        conflictResolutionHandler =  new ConflictResolutionHandler(this);
        this.conflictResolver = conflictResolver;
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> void retryConflictMutation(@Nonnull Mutation<D, T, V> mutation, String uniqueIdentifierForOriginalMutation) {

        //Remove callback from map. This will return null for persistent offline mutations
        InterceptorCallback callback = (InterceptorCallback) callbackMapForInMemoryMutations.remove(uniqueIdentifierForOriginalMutation);
        if (callback != null ) {
            //Put the callback in with the mutation.toString() as the key. This will be picked up to route the the mutation results
            // back to the callback later.
            Log.d(TAG, "Proceeding with retry for inMemory offline mutation [" + uniqueIdentifierForOriginalMutation + "]");
            callbackMapForInMemoryMutations.put(mutation.toString(), callback);
        }
        else {
            // put in details of persistent mutation.
            //TODO: Check if this logic is required.
            Log.d(TAG, "Proceeding with retry for persistent offline mutation [" + uniqueIdentifierForOriginalMutation + "]");
            if (persistentOfflineMutationObjectMap.isEmpty()) {
                Log.d(TAG, "Populating mutations map.");
                persistentOfflineMutationObjectMap.putAll(appSyncOfflineMutationManager.persistentOfflineMutationManager.persistentOfflineMutationObjectMap);
            }
            PersistentOfflineMutationObject object = persistentOfflineMutationObjectMap.remove(uniqueIdentifierForOriginalMutation);
            persistentOfflineMutationObjectMap.put(mutation.toString(), object);
        }
        appSyncClient.mutate(mutation, true).enqueue(null);
    }

    public void failConflictMutation(String identifier) {
        ConflictResolutionFailedException e = new ConflictResolutionFailedException("Mutation [" + identifier + "] failed due to conflict");
        //Attempt callback from map that houses callbacks for inMemory Mutations
        ApolloInterceptor.CallBack callback = callbackMapForInMemoryMutations.get(identifier);
        if ( callback != null ) {
            //Invoke onFailure and remove callback from map
            callback.onFailure(e);
            callbackMapForInMemoryMutations.remove(identifier);
        }
        else {
            final PersistentMutationsCallback callbackForPersistentMutation =
                    appSyncOfflineMutationManager.persistentOfflineMutationManager.networkInvoker.persistentMutationsCallback;
            if (callbackForPersistentMutation != null ) {
                callbackForPersistentMutation.onFailure(
                        new PersistentMutationsError(
                                queueHandler.persistentOfflineMutationObjectBeingExecuted.getClass().getSimpleName(),
                                identifier,
                                e
                        )
                );
            }
        }

        //Remove the Mutation and Callback from the inMemory Maps
        mutationsToRetryAfterConflictResolution.remove(identifier);

        //Set the QueueHandler State
        if (queueHandler.persistentOfflineMutationObjectBeingExecuted != null ) {
            appSyncOfflineMutationManager.setInProgressPersistentMutationAsCompleted(identifier);
        }
        else {
            appSyncOfflineMutationManager.setInProgressMutationAsCompleted(identifier);
        }
        queueHandler.clearPersistentOfflineMutationObjectBeingExecuted();
        queueHandler.clearInMemoryOfflineMutationObjectBeingExecuted();

        //Signal next in Queue to be processed
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

        //Mutations that go through the conflict resolution handler will be present in the mutationsToRetryAfterConflictResolution map.
        if (!mutationsToRetryAfterConflictResolution.containsKey(request.operation)) {
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:Nope, hasn't encountered  conflict");

            //Create a callback to inspect the results before calling caller provided callback.
            ApolloInterceptor.CallBack customCallBack = new InterceptorCallback(
                    callBack,
                    queueHandler,
                    (Mutation) request.operation,
                    (Mutation) request.operation,
                    appSyncOfflineMutationManager.getClientStateFromMutation((Mutation) request.operation),
                    request.uniqueId.toString(),
                    appSyncOfflineMutationManager);

            try {
                //Add the custom callback to the in memoryInterceptorCallback map
                callbackMapForInMemoryMutations.put(request.uniqueId.toString(), customCallBack);

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
            return;
        }

        //This is a mutation that is being retried after the conflict resolution handler has addressed the conflict.
        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Yes, this is a mutation that gone through conflict resolution. Executing it.");

        //Remove from the map. We only do one retry.
        mutationsToRetryAfterConflictResolution.remove(request.operation);

        //Get the callback associated with the first attempt of this mutation.
        Log.v(TAG, "Looking up originalCallback using key[" + request.operation.toString() + "]");
        InterceptorCallback callbackForInMemoryMutation = (InterceptorCallback) callbackMapForInMemoryMutations.get(request.operation.toString());

        if (callbackForInMemoryMutation != null ) {
            Log.v(TAG, "callback found. Proceeding to execute inMemory offline mutation");
            //Execute inMemory Mutation
            chain.proceedAsync(request,dispatcher,callbackForInMemoryMutation);
            return;
        }

        // Original callback was null. This is due to the mutation being a persistent offline mutation which is being retried
        final PersistentMutationsCallback callbackForPersistentMutation =
                appSyncOfflineMutationManager.persistentOfflineMutationManager.networkInvoker.persistentMutationsCallback;

        final PersistentOfflineMutationObject object = persistentOfflineMutationObjectMap.get(request.operation.toString());
        Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Fetched object: " + object);

        chain.proceedAsync(request, dispatcher, new CallBack() {

            @Override
            public void onResponse(@Nonnull InterceptorResponse response) {
                callBack.onResponse(response);

                if ( callbackForPersistentMutation != null) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = new JSONObject(response.clonedBufferString.get());

                        callbackForPersistentMutation.onResponse(new PersistentMutationsResponse(
                                jsonObject.getJSONObject("data"),
                                jsonObject.getJSONArray("errors"),
                                request.operation.getClass().getSimpleName(),
                                object.recordIdentifier));

                    } catch (Exception e) {
                        callbackForPersistentMutation.onFailure(new PersistentMutationsError(
                                request.operation.getClass().getSimpleName(),
                                object.recordIdentifier,
                                new ApolloParseException(e.getLocalizedMessage()))
                        );
                    }
                }
                appSyncOfflineMutationManager.setInProgressPersistentMutationAsCompleted(object.recordIdentifier);
                queueHandler.clearInMemoryOfflineMutationObjectBeingExecuted();
                queueHandler.clearPersistentOfflineMutationObjectBeingExecuted();
                queueHandler.sendEmptyMessage(MessageNumberUtil.SUCCESSFUL_EXEC);
            }

            @Override
            public void onFetch(FetchSourceType sourceType) {
                callBack.onFetch(sourceType);
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                callBack.onFailure(e);
                if ( callbackForPersistentMutation != null) {
                    callbackForPersistentMutation.onFailure(
                            new PersistentMutationsError(request.operation.getClass().getSimpleName(),
                                    object.recordIdentifier,
                                    e));
                }
                appSyncOfflineMutationManager.setInProgressPersistentMutationAsCompleted(object.recordIdentifier);
                queueHandler.clearPersistentOfflineMutationObjectBeingExecuted();
                queueHandler.clearInMemoryOfflineMutationObjectBeingExecuted();
                queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
            }

            @Override
            public void onCompleted() {
            }
        });

    }

    @Override
    public void dispose() {
        // do nothing
        Log.v(TAG, "Dispose called");
    }

    //The AppSyncOfflineMutationInterceptor is a shared object used by all API calls moving through the chain
    //and does not have state per mutation.
    //This method is needed to ensure that we dispose the correct mutation
    public void dispose(Mutation mutation) {
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Dispose called for mutation [" + mutation + "]." );
        appSyncOfflineMutationManager.handleMutationCancellation(mutation);
    }

}
