/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
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
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Delta Sync: App is in FOREGROUND");
        AWSAppSyncDeltaSync.handleAppForeground();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopSomething() {
        Log.v(TAG, "Thread:[" + Thread.currentThread().getId() +"]: Delta Sync: App is in BACKGROUND");
        AWSAppSyncDeltaSync.handleAppBackground();
    }
}
