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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PersistentOfflineMutationManager.
 */
public class PersistentOfflineMutationManager {
    private static final String TAG = PersistentOfflineMutationManager.class.getSimpleName();

    final AppSyncMutationSqlCacheOperations mutationSqlCacheOperations;
    final AppSyncCustomNetworkInvoker networkInvoker;
    AppSyncOfflineMutationInterceptor.QueueUpdateHandler queueHandler;
    List<PersistentOfflineMutationObject> persistentOfflineMutationObjectList;
    Map<String, PersistentOfflineMutationObject> persistentOfflineMutationObjectMap;
    Set<PersistentOfflineMutationObject> timedOutMutations;

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
        timedOutMutations = new HashSet<PersistentOfflineMutationObject>();

        networkInvoker.setPersistentOfflineMutationManager(this);
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]:Exiting the constructor. There are [" + persistentOfflineMutationObjectList.size() + "] mutations in the persistent queue");
    }

    void updateQueueHandler(AppSyncOfflineMutationInterceptor.QueueUpdateHandler queueHandler) {
        this.queueHandler = queueHandler;
        networkInvoker.updateQueueHandler(queueHandler);
    }

    //Remove mutation request from persistent store
    public synchronized boolean removePersistentMutationObject(final String recordId) {
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:Removing mutation [" + recordId +"] from persistent store");
        if (persistentOfflineMutationObjectList.size() > 0) {
            PersistentOfflineMutationObject mutationObject = persistentOfflineMutationObjectList.get(0);
            if (recordId.equalsIgnoreCase(mutationObject.recordIdentifier)) {
                persistentOfflineMutationObjectList.remove(0);
            }
        }
        mutationSqlCacheOperations.deleteRecord(recordId);
        return true;
    }

    //Add mutation request to persistent store
    public synchronized void addPersistentMutationObject(PersistentOfflineMutationObject mutationObject) {
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
       Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]: Fetching all mutation requests from persistent store");
        return mutationSqlCacheOperations.fetchAllRecords();
    }

    //Return true if Queue is empty, false otherwise.
    public synchronized boolean isQueueEmpty() {
        return persistentOfflineMutationObjectList.isEmpty();
    }

    public PersistentOfflineMutationObject processNextMutationObject() {
       Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:In processNextMutationObject");
        PersistentOfflineMutationObject mutationRequestObject = getFirstInQueue();
        if ( mutationRequestObject != null ) {
            // kick off originalMutation here through the custom network invoker
            networkInvoker.executeRequest(mutationRequestObject);
        }
        return mutationRequestObject;
    }


    private synchronized PersistentOfflineMutationObject getFirstInQueue() {
        Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:In getFirstInQueue");
        if (persistentOfflineMutationObjectList.size() > 0) {
            PersistentOfflineMutationObject mutationObject = persistentOfflineMutationObjectList.get(0);
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:returning mutation[" + mutationObject.recordIdentifier + "]: " + mutationObject.responseClassName + " \n\n " + mutationObject.requestString);
            return mutationObject;
        }
        return null;
    }

    public synchronized void addTimedoutMutation(PersistentOfflineMutationObject p) {
        timedOutMutations.add(p);
    }

    public synchronized void removeTimedoutMutation(PersistentOfflineMutationObject p ) {
        timedOutMutations.remove(p);
    }

    public synchronized  Set<PersistentOfflineMutationObject> getTimedoutMutations() {
        return timedOutMutations;
    }
}
