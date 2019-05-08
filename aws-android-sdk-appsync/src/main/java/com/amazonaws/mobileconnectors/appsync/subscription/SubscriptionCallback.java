/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.subscription;

public interface SubscriptionCallback {
    void onMessage(String topic, String message);

    void onError(String topic, Exception e);
}
