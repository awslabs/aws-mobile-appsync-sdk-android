/**
 * Copyright 2018-2019 Amazon.com,
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

import com.apollographql.apollo.api.Mutation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * InMemoryOfflineMutationManager.
 */

public class InMemoryOfflineMutationManager {
    private static final String TAG = InMemoryOfflineMutationManager.class.getSimpleName();

    //Use a linked list to model the inMemory Queue
    List<InMemoryOfflineMutationObject> inMemoryOfflineMutationObjects = new LinkedList<>();
    Set<Mutation> cancelledMutations = new HashSet<>();

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

    public InMemoryOfflineMutationObject removeFromQueue(String recordIdentifer) {
        synchronized ( lock ) {
            if (!inMemoryOfflineMutationObjects.isEmpty() ) {
                InMemoryOfflineMutationObject mutationObject = inMemoryOfflineMutationObjects.get(0);
                if (mutationObject != null && recordIdentifer.equals(mutationObject.recordIdentifier)) {
                    return inMemoryOfflineMutationObjects.remove(0);
                }
            }
        }
        return null;
    }

    public InMemoryOfflineMutationObject processNextMutation() {
        InMemoryOfflineMutationObject offlineMutationObject = getFirstInQueue();

        //Only execute if not canceled
        if (offlineMutationObject != null && !getCancelledMutations().contains(offlineMutationObject.request.operation )) {
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]:Executing mutation [" +  offlineMutationObject.recordIdentifier +"]");
            offlineMutationObject.execute();
        }
        return offlineMutationObject;
    }

    private InMemoryOfflineMutationObject getFirstInQueue() {
        synchronized (lock ) {
            if (!inMemoryOfflineMutationObjects.isEmpty() ) {
                return inMemoryOfflineMutationObjects.get(0);
            }
        }
        return null;
    }

    void addCancelledMutation(Mutation m) {
        synchronized (lock) {
            cancelledMutations.add(m);
        }
    }

    Set<Mutation> getCancelledMutations() {
        synchronized (lock) {
            return cancelledMutations;
        }
    }

    void removeCancelledMutation(Mutation m) {
        synchronized (lock) {
            cancelledMutations.remove(m);
        }
    }

    InMemoryOfflineMutationObject getMutationObject(Mutation m) {
        for(InMemoryOfflineMutationObject inMemoryOfflineMutationObject: inMemoryOfflineMutationObjects) {
            if (inMemoryOfflineMutationObject.equals(m)) {
                return inMemoryOfflineMutationObject;
            }
        }
        return null;
    }

    void clearMutationQueue() {
        synchronized (lock) {
            inMemoryOfflineMutationObjects.clear();
            cancelledMutations.clear();
        }
    }
}
