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

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.internal.response.OperationResponseParser;

import java.util.List;

import okio.BufferedSource;

public class AppSyncSubscription {
    Subscription call;
    OperationResponseParser parser;

    private AppSyncSubscription(Builder builder) {
        call = builder.call;
        parser = createMessageParser(call);
    }

    private OperationResponseParser createMessageParser(Subscription call) {
        return new OperationResponseParser(
                call,
                null,//responseFieldMapper,
                null,//scalarTypeAdapters,
                null);
    }

    public void parse(BufferedSource source) {
        try {
            this.parser.parse(source);
        } catch (Exception e) {
            Log.w("TAG", "Failed to parse subscription", e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        Subscription call;
        List<String> topics;

        protected Builder() {

        }

        public Builder call(Subscription call) {
            this.call = call;
            return this;
        }

        public Builder topics(List<String> topics) {
            this.topics = topics;
            return this;
        }

        public AppSyncSubscription build() {
            return new AppSyncSubscription(this);
        }
    }
}
