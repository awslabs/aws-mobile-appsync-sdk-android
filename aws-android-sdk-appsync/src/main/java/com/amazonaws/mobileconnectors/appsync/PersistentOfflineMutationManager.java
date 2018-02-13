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

import java.util.LinkedList;
import java.util.List;

import static com.amazonaws.mobileconnectors.appsync.AppSyncOfflineMutationManager.MSG_EXEC;

/**
 * PersistentOfflineMutationManager.
 */

public class PersistentOfflineMutationManager {

    final AppSyncMutationSqlCacheOperations mutationSqlCacheOperations;
    final AppSyncCustomNetworkInvoker networkInvoker;

    List<PersistentOfflineMutationObject> persistentOfflineMutationObjects;

    public PersistentOfflineMutationManager(AppSyncMutationSqlCacheOperations mutationSqlCacheOperations,
                                            AppSyncCustomNetworkInvoker networkInvoker) {
        this.mutationSqlCacheOperations = mutationSqlCacheOperations;
        this.networkInvoker = networkInvoker;
        persistentOfflineMutationObjects = fetchPersistentMutationsList();
        Log.d("AppSync", "There these many records in persistent cache: " + persistentOfflineMutationObjects.size());
    }


    public boolean removePersistentMutationObject(final String recordId) {
        mutationSqlCacheOperations.deleteRecord(recordId);
        return true;
    }

    public void addPersistentMutationObject(PersistentOfflineMutationObject mutationObject) {
        Log.d("AppSync","Adding object: " + mutationObject.responseClassName + " \n\n " + mutationObject.requestString);
        mutationSqlCacheOperations.createRecord(mutationObject.recordIdentifier, mutationObject.requestString, mutationObject.responseClassName);
        // TODO: Persist ONLY here. Do not add in queue.
    }

    public List<PersistentOfflineMutationObject> fetchPersistentMutationsList() {
        // TODO: Load in the queue here. InMemory ones will be processed from separate queue.
        return mutationSqlCacheOperations.fetchAllRecords();
    }

    public boolean isQueueEmpty() {
        return persistentOfflineMutationObjects.isEmpty();
    }

    public PersistentOfflineMutationObject processNextMutationObject() {
        PersistentOfflineMutationObject offlineMutationObject = removeAndGetLastInQueue();
        networkInvoker.executeRequest(offlineMutationObject);
        // kick off mutation here through custom flow
        return offlineMutationObject;
    }

    public PersistentOfflineMutationObject removeAndGetLastInQueue() {
        if (persistentOfflineMutationObjects.size() >= 1) {
            PersistentOfflineMutationObject mutationObject = persistentOfflineMutationObjects.remove(0);
            mutationSqlCacheOperations.deleteRecord(mutationObject.recordIdentifier);
            return mutationObject;
        }
        throw new IllegalStateException("Persistent Mutation Queue is empty. Cannot remove object.");
    }
}
