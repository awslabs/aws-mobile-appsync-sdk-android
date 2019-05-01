/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.subscription;

import java.util.Set;

public interface SubscriptionClient {
    void connect(SubscriptionClientCallback callback);
    void subscribe(String topic, int qos, SubscriptionCallback callback);
    void unsubscribe(String topic);
    void setTransmitting(boolean isTransmitting);
    public Set<String> getTopics();
    void close();
}
