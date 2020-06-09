/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserStateDetails;

import java.util.Objects;

/**
 * A wrapper of the {@link AWSMobileClient} which maintains a singleton of the client.
 * The singleton instance of the client is initialized in a synchronous way, and is
 * usable as soon as it is returned.
 */
final class TestAWSMobileClient {
    private static AWSMobileClient instance;

    static synchronized AWSMobileClient instance(@NonNull Context context) {
        Objects.requireNonNull(context);
        if (instance == null) {
            LatchedCallback<UserStateDetails> callback = LatchedCallback.instance();
            AWSMobileClient.getInstance().initialize(context, callback);
            callback.awaitResult();
            instance = AWSMobileClient.getInstance();
        }
        return instance;
    }
}
