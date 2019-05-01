/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.subscription;

public interface SubscriptionListener<T> {
    void onMessage(T message);
    void onError(Exception e);
}
