/**
 * Copyright 2019-2019 Amazon.com,
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

public class ClearCacheOptions {
    private boolean queries;
    private boolean mutations;
    private boolean subscriptions;

    private ClearCacheOptions(boolean queries, boolean mutations, boolean subscriptions) {
        this.queries = queries;
        this.mutations = mutations;
        this.subscriptions = subscriptions;
    }

    public static class Builder {
        private boolean queries = false;
        private boolean mutations = false;
        private boolean subscriptions = false;

        public Builder clearQueries() {
            this.queries = true;
            return this;
        }

        public Builder clearMutations() {
            this.mutations = true;
            return this;
        }

        public Builder clearSubscriptions() {
            this.subscriptions = true;
            return this;
        }

        public ClearCacheOptions build() {
            return new ClearCacheOptions(queries, mutations, subscriptions);
        }
    }

    boolean isQueries() {
        return queries;
    }

    boolean isMutations() {
        return mutations;
    }

    boolean isSubscriptions() {
        return subscriptions;
    }

    public static Builder builder() {
        return new Builder();
    }
}
