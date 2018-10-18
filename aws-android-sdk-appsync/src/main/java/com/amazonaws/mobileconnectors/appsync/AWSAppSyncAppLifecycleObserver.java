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

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.util.Log;

public class AWSAppSyncAppLifecycleObserver implements LifecycleObserver {
    //Constant for Logging
    private static final String TAG = AWSAppSyncDeltaSync.class.getSimpleName();

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startSomething() {
        Log.v(TAG, "Delta Sync: App is in FOREGROUND");
        AWSAppSyncDeltaSync.handleAppForeground();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopSomething() {
        Log.v(TAG, "Delta Sync: App is in BACKGROUND");
        AWSAppSyncDeltaSync.handleAppBackground();
    }
}
