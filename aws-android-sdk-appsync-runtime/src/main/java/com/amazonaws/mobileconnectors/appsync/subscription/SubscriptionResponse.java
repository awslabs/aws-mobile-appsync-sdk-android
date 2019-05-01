/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.subscription;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionResponse {
    public List<MqttInfo> mqttInfos = new ArrayList<>();

    public void add(MqttInfo info) {
        mqttInfos.add(info);
    }

    public static class MqttInfo {
        public MqttInfo(String clientId, String wssURL, String[] topics) {
            this.clientId = clientId;
            this.wssURL = wssURL;
            this.topics = topics;
        }
        public String clientId;
        public String wssURL;
        public String[] topics;
    }
}
