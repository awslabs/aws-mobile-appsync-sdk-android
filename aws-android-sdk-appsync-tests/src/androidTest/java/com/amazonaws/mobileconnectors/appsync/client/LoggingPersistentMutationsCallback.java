/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.client;

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.PersistentMutationsCallback;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsError;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsResponse;

final class LoggingPersistentMutationsCallback implements PersistentMutationsCallback {
    private static final String TAG = LoggingPersistentMutationsCallback.class.getName();

    private LoggingPersistentMutationsCallback() {}

    static LoggingPersistentMutationsCallback instance() {
        return new LoggingPersistentMutationsCallback();
    }

    @Override
    public void onResponse(PersistentMutationsResponse response) {
        Log.d(TAG, response.getMutationClassName());
    }

    @Override
    public void onFailure(PersistentMutationsError error) {
        Log.e(TAG, error.getMutationClassName());
        Log.e(TAG, "Error", error.getException());
    }
}
