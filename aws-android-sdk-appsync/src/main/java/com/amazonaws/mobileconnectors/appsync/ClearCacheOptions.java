/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

/**
 * Options object that indicates what client databases (caches)
 * to clear when passed in to AWSAppSyncClient#clearCache(ClearCacheOptions).
 */
public class ClearCacheOptions {
    private boolean queries;
    private boolean mutations;
    private boolean subscriptions;

    /**
     * Constructor invoked by the builder.
     *
     * @param queries true indicates clear the query responses in query cache
     * @param mutations true indicates clear the offline persistent mutations
     * @param subscriptions true indicates clear the subscriptions metadata stored by Delta Sync
     */
    private ClearCacheOptions(boolean queries, boolean mutations, boolean subscriptions) {
        this.queries = queries;
        this.mutations = mutations;
        this.subscriptions = subscriptions;
    }

    /**
     * Builder class used for constructing ClearCacheOptions object.
     */
    public static class Builder {
        private boolean queries = false;
        private boolean mutations = false;
        private boolean subscriptions = false;

        /**
         * Clear the query cache
         * @return builder object
         */
        public Builder clearQueries() {
            this.queries = true;
            return this;
        }

        /**
         * Clear the mutation queue
         * @return builder object
         */
        public Builder clearMutations() {
            this.mutations = true;
            return this;
        }

        /**
         * Clear the subscriptions metadata stored by Delta Sync
         * @return builder object
         */
        public Builder clearSubscriptions() {
            this.subscriptions = true;
            return this;
        }

        /**
         * Build the ClearCacheOptions object based on the builder
         * @return ClearCacheOptions object
         */
        public ClearCacheOptions build() {
            return new ClearCacheOptions(queries, mutations, subscriptions);
        }
    }

    /**
     * @return true if clearQueries() was called
     */
    boolean isQueries() {
        return queries;
    }

    /**
     * @return true if clearMutations() was called
     */
    boolean isMutations() {
        return mutations;
    }

    /**
     * @return true if clearSubscriptions() was called
     */
    boolean isSubscriptions() {
        return subscriptions;
    }

    /**
     * @return the builder object
     */
    public static Builder builder() {
        return new Builder();
    }
}
