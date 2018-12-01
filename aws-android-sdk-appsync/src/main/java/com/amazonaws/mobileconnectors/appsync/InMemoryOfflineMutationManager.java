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
 * InMemoryOfflineMutationManager.
 */

public class InMemoryOfflineMutationManager {
    private static final String TAG = InMemoryOfflineMutationManager.class.getSimpleName();

    //Use a linked list to model the inMemory Queue
    List<InMemoryOfflineMutationObject> inMemoryOfflineMutationObjects = new LinkedList<>();

    //lock object to make the methods thread safe.
    Object lock  = new Object();

    public boolean isQueueEmpty() {
        synchronized (lock) {
            return inMemoryOfflineMutationObjects.isEmpty();
        }
    }

    public void addMutationObjectInQueue(InMemoryOfflineMutationObject object) {
        synchronized (lock) {
            inMemoryOfflineMutationObjects.add(object);
        }
    }

    public InMemoryOfflineMutationObject removeAndGetLastInQueue() {
        synchronized ( lock ) {
            if (!inMemoryOfflineMutationObjects.isEmpty() ) {
                return inMemoryOfflineMutationObjects.remove(0);
            }
        }
        return null;
    }

    public InMemoryOfflineMutationObject processNextMutation() {
        InMemoryOfflineMutationObject offlineMutationObject = removeAndGetLastInQueue();
        if (offlineMutationObject != null ) {
            Log.v(TAG,"Thread:[" + Thread.currentThread().getId() +"]:Sending MSG_EXEC to mutation [" +  offlineMutationObject.recordIdentifier +"]");
            offlineMutationObject.handler.sendEmptyMessage(MSG_EXEC);
        }
        return offlineMutationObject;
    }
}
