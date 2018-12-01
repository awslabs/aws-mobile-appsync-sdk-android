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
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PersistentOfflineMutationManager.
 */
public class PersistentOfflineMutationManager {
    private static final String TAG = PersistentOfflineMutationManager.class.getSimpleName();

    final AppSyncMutationSqlCacheOperations mutationSqlCacheOperations;
    final AppSyncCustomNetworkInvoker networkInvoker;
    Handler queueHandler;
    List<PersistentOfflineMutationObject> persistentOfflineMutationObjectList;
    Map<String, PersistentOfflineMutationObject> persistentOfflineMutationObjectMap;

    public PersistentOfflineMutationManager(AppSyncMutationSqlCacheOperations mutationSqlCacheOperations,
                                            AppSyncCustomNetworkInvoker networkInvoker) {
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:In Constructor");
        this.mutationSqlCacheOperations = mutationSqlCacheOperations;
        this.networkInvoker = networkInvoker;

        //Get all the previously persisted mutations and house them in the persistentOfflineMutationObjectList and persistentOfflineMutationObjectMap
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:Priming the pump - Fetching all the queued mutations from the persistent store");
        persistentOfflineMutationObjectList = fetchPersistentMutationsList();
        persistentOfflineMutationObjectMap = new HashMap<>();
        for (PersistentOfflineMutationObject object: persistentOfflineMutationObjectList) {
            persistentOfflineMutationObjectMap.put(object.recordIdentifier, object);
        }

        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]:Exiting the constructor. There are [" + persistentOfflineMutationObjectList.size() + "] mutations in the persistent queue");
    }

    void updateQueueHandler(Handler queueHandler) {
        this.queueHandler = queueHandler;
        networkInvoker.updateQueueHandler(queueHandler);
    }

    //Remove mutation request from persistent store
    public boolean removePersistentMutationObject(final String recordId) {
       Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:Removing mutation [" + recordId +"] from persistent store");
        mutationSqlCacheOperations.deleteRecord(recordId);
        return true;
    }

    //Add mutation request to persistent store
    public void addPersistentMutationObject(PersistentOfflineMutationObject mutationObject) {
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:addPersistentMutationObject: Adding mutation[" + mutationObject.recordIdentifier + "]: " + mutationObject.responseClassName + " \n" + mutationObject.requestString);
        mutationSqlCacheOperations.createRecord(mutationObject.recordIdentifier,
                mutationObject.requestString,
                mutationObject.responseClassName,
                mutationObject.clientState,
                mutationObject.bucket,
                mutationObject.key,
                mutationObject.region,
                mutationObject.localURI,
                mutationObject.mimeType);
    }

    //Load mutation requests from persistent store
    public List<PersistentOfflineMutationObject> fetchPersistentMutationsList() {
       Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:Fetching all mutation requests from persistent store");
        return mutationSqlCacheOperations.fetchAllRecords();
    }

    //Return true if Queue is empty, false otherwise.
    public boolean isQueueEmpty() {
        return persistentOfflineMutationObjectList.isEmpty();
    }

    public PersistentOfflineMutationObject processNextMutationObject() {
       Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:In processNextMutationObject");
        PersistentOfflineMutationObject mutationRequestObject = removeAndGetLastInQueue();
        if ( mutationRequestObject != null ) {

            //TODO:Do this in the callback
            removePersistentMutationObject(mutationRequestObject.recordIdentifier);
            // kick off originalMutation here through custom flow
            networkInvoker.executeRequest(mutationRequestObject);

        }
        return mutationRequestObject;
    }

    public PersistentOfflineMutationObject removeAndGetLastInQueue() {

        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:In removeAndGetLastInQueue");
        if (persistentOfflineMutationObjectList.size() > 0) {
            PersistentOfflineMutationObject mutationObject = persistentOfflineMutationObjectList.remove(0);
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:returning mutation[" + mutationObject.recordIdentifier + "]: " + mutationObject.responseClassName + " \n\n " + mutationObject.requestString);
            return mutationObject;
        }
        return null;
    }
}
