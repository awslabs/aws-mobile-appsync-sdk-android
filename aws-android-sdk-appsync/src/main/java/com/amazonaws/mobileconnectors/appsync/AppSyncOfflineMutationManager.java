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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.S3InputObjectInterface;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.internal.json.InputFieldJsonWriter;
import com.apollographql.apollo.internal.json.JsonWriter;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okio.Buffer;

/**
 * AppSyncOfflineMutationManager.
 */
class AppSyncOfflineMutationManager {

    /*
     * Constants of message sent to update handler.
     */
    static final int MSG_EXEC = 100;
    static final int MSG_CHECK = 200;
    static final int MSG_DISCONNECT = 300;
    private static final String TAG = AppSyncOfflineMutationManager.class.getSimpleName();
    private NetworkUpdateHandler networkUpdateHandler;
    private HandlerThread handlerThread;

    private Object shouldProcessMutationsLock = new Object();
    private boolean shouldProcessMutations;


    InMemoryOfflineMutationManager inMemoryOfflineMutationManager;
    PersistentOfflineMutationManager persistentOfflineMutationManager;
    private ScalarTypeAdapters scalarTypeAdapters;
    private AppSyncMutationSqlCacheOperations mutationSqlCacheOperations;
    private AppSyncOfflineMutationInterceptor.QueueUpdateHandler queueHandler;
    private Context context;

    //Constructor
    public AppSyncOfflineMutationManager(Context context,
                                         final Map<ScalarType, CustomTypeAdapter> customTypeAdapters,
                                         final AppSyncMutationSqlCacheOperations mutationSqlCacheOperations,
                                         final AppSyncCustomNetworkInvoker persistentMutationsNetworkInvoker) {
        this.context = context;

        //Setup HandlerThread
        handlerThread = new HandlerThread(TAG + "-AWSAppSyncOfflineMutationsHandlerThread");
        handlerThread.start();


        //Setup InMemory and Persistent Queue
        this.inMemoryOfflineMutationManager = new InMemoryOfflineMutationManager();
        persistentOfflineMutationManager = new PersistentOfflineMutationManager(mutationSqlCacheOperations,
                persistentMutationsNetworkInvoker);

        //Setup Network Monitoring objects
        this.networkUpdateHandler = new NetworkUpdateHandler(handlerThread.getLooper());
        this.networkInfoReceiver = new NetworkInfoReceiver(context, this.networkUpdateHandler);
        context.getApplicationContext().registerReceiver(networkInfoReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));

        //Setup ancillary stuff
        this.scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);
        this.mutationSqlCacheOperations = mutationSqlCacheOperations;
    }


    void updateQueueHandler(AppSyncOfflineMutationInterceptor.QueueUpdateHandler queueHandler) {
        this.queueHandler = queueHandler;
        this.persistentOfflineMutationManager.updateQueueHandler(queueHandler);
    }

    /*
     * Registers a BroadcastReceiver to receive network status change events. It
     * will update transfer records in database directly.
     */
    private NetworkInfoReceiver networkInfoReceiver;

    public boolean shouldProcess() {
        return shouldProcessMutations;
    }

    public void addMutationObjectInQueue(InMemoryOfflineMutationObject mutationObject) throws IOException {

        inMemoryOfflineMutationManager.addMutationObjectInQueue(mutationObject);
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:  Added mutation[" + mutationObject.recordIdentifier + "] to inMemory Queue"  );

        S3InputObjectInterface s3InputObjectInterface = getS3ComplexObject(mutationObject.request.operation.variables().valueMap());
        if (s3InputObjectInterface == null) {
            persistentOfflineMutationManager.addPersistentMutationObject(
                    new PersistentOfflineMutationObject(
                            mutationObject.recordIdentifier,
                            httpRequestBody(mutationObject.request.operation),
                            mutationObject.request.operation.getClass().getSimpleName(),
                            new JSONObject(mutationObject.request.operation.variables().valueMap()).toString())
            );
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Added mutation[" + mutationObject.recordIdentifier + "] to Persistent Queue. No S3 Objects found"  );

        }
        else {
            persistentOfflineMutationManager.addPersistentMutationObject(
                    new PersistentOfflineMutationObject(
                            mutationObject.recordIdentifier,
                            httpRequestBody(mutationObject.request.operation),
                            mutationObject.request.operation.getClass().getSimpleName(),
                            new JSONObject(mutationObject.request.operation.variables().valueMap()).toString(),
                            s3InputObjectInterface.bucket(),
                            s3InputObjectInterface.key(),
                            s3InputObjectInterface.region(),
                            s3InputObjectInterface.localUri(),
                            s3InputObjectInterface.mimeType()));
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Added mutation[" + mutationObject.recordIdentifier + "] to Persistent Queue. S3 Object found"  );

        }
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Created both in-memory and persistent records. Now going to signal queuehanlder.");

        //Execute next mutation in Queue
        Message message = new Message();
        message.obj = new MutationInterceptorMessage();
        message.what = MessageNumberUtil.SUCCESSFUL_EXEC;
        queueHandler.sendMessage(message);
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

    public void processNextInQueueMutation() {
        synchronized (shouldProcessMutationsLock) {
            if (!shouldProcessMutations ) {
                Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Internet wasn't available. Exiting");
                return;
            }
        }

        //Double check to make sure we do have network connectivity, as there can be latency in
        // the propagation of the network state through the broadcast receiver.
        final NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info != null && !info.isConnected()) {
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Internet wasn't available. Exiting");
            return;
        }

        //Internet is available. Check if there is anything in the queue to process
        if (!persistentOfflineMutationManager.isQueueEmpty()) {
            if (queueHandler.setMutationInProgress()) {
                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Processing next from persistent queue");
                persistentOfflineMutationManager.processNextMutationObject();
            }
            return;
        }

        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:Persistent mutations queue is EMPTY!. Will check inMemory Queue next");
        if (!inMemoryOfflineMutationManager.isQueueEmpty()) {
            if (queueHandler.setMutationInProgress()) {
                Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Processing next from in Memory queue");
                inMemoryOfflineMutationManager.processNextMutation();
            }
        }
        else {
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: In Memory mutations queue was EMPTY!. Nothing to process, exiting");
        }
    }

    public void setInProgressMutationAsCompleted(String recordIdentifier) {
        persistentOfflineMutationManager.removePersistentMutationObject(recordIdentifier);
        inMemoryOfflineMutationManager.removeFirstInQueue();
        queueHandler.setMutationInProgressStatusToFalse();
    }

    // Handler that processes the message sent by the NetworkInfoReceiver to kick off mutations
    // TODO: Investigate if this can be simplified by just invoking the QueueHandler in the network receiver.
    class NetworkUpdateHandler extends Handler {
        public NetworkUpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CHECK) {
                // remove messages of the same type
                networkUpdateHandler.removeMessages(MSG_CHECK);

                // set shouldProcess to true
                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Internet CONNECTED.");
                synchronized (shouldProcessMutationsLock) {
                    shouldProcessMutations = true;
                }

                // Send EXEC message to queueHandler
                if (queueHandler != null ) {
                    Message message = new Message();
                    message.obj = new MutationInterceptorMessage();
                    message.what = MessageNumberUtil.SUCCESSFUL_EXEC;
                    queueHandler.sendMessage(message);
                }

                //Trigger DeltaSync to handle Network up events.
                AWSAppSyncDeltaSync.handleNetworkUpEvent();

            } else if (msg.what == MSG_DISCONNECT) {
                // Network Disconnected, pause mutations
                Log.d(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Internet DISCONNECTED.");
                synchronized (shouldProcessMutationsLock) {
                    shouldProcessMutations = false;
                }
                //Propagate network down event to DeltaSync
                AWSAppSyncDeltaSync.handleNetworkDownEvent();
            }
        }
    }


    /**
     * A Broadcast receiver to receive network connection change events.
     */
    static class NetworkInfoReceiver extends BroadcastReceiver {
        private final Handler handler;
        private final ConnectivityManager connManager;

        /**
         * Constructs a NetworkInfoReceiver.
         *
         * @param handler a handle to send message to
         */
        public NetworkInfoReceiver(Context context, Handler handler) {
            this.handler = handler;
            connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                final boolean networkConnected = isNetworkConnected();
               handler.sendEmptyMessage(networkConnected ? MSG_CHECK : MSG_DISCONNECT);
            }
        }

        /**
         * Gets the status of network connectivity.
         *
         * @return true if network is connected, false otherwise.
         */
        boolean isNetworkConnected() {
            final NetworkInfo info = connManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    // To be used for offline mutations which are persisted across app restarts
    private String httpRequestBody(Operation operation) throws IOException {
        Buffer buffer = new Buffer();
        JsonWriter jsonWriter = JsonWriter.of(buffer);
        jsonWriter.beginObject();
        jsonWriter.name("query").value(operation.queryDocument().replaceAll("\\n", ""));
        jsonWriter.name("variables").beginObject();
        operation.variables().marshaller().marshal(new InputFieldJsonWriter(jsonWriter, scalarTypeAdapters));
        jsonWriter.endObject();
        jsonWriter.endObject();
        jsonWriter.close();
        return buffer.readUtf8();
    }
}

