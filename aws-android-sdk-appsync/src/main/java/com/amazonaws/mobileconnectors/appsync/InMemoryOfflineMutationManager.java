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

import java.util.LinkedList;
import java.util.List;

import static com.amazonaws.mobileconnectors.appsync.AppSyncOfflineMutationManager.MSG_EXEC;

/**
 * InMemoryOfflineMutationManager.
 */

public class InMemoryOfflineMutationManager {
    List<InMemoryOfflineMutationObject> inMemoryOfflineMutationObjects = new LinkedList<>();

    public boolean isQueueEmpty() {
        return inMemoryOfflineMutationObjects.isEmpty();
    }

    public void addMutationObjectInQueue(InMemoryOfflineMutationObject object) {
        inMemoryOfflineMutationObjects.add(object);
    }

    public InMemoryOfflineMutationObject removeAndGetLastInQueue() {
        if (inMemoryOfflineMutationObjects.size() >= 1) {
            return inMemoryOfflineMutationObjects.remove(0);
        }
        throw new IllegalStateException("InMemory Mutation Queue is empty. Cannot remove object.");
    }

    public InMemoryOfflineMutationObject processNextMutation() {
        InMemoryOfflineMutationObject offlineMutationObject = removeAndGetLastInQueue();
        offlineMutationObject.handler.sendEmptyMessage(MSG_EXEC);
        return offlineMutationObject;
    }
}
