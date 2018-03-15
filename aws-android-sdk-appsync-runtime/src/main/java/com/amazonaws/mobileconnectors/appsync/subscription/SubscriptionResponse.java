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
