/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * This package contains code for interaction with an
 * {@link com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient},
 * itself. The {@link com.amazonaws.mobileconnectors.appsync.client.AWSAppSyncClients}
 * is a factory class to create various instances of the client,
 * permuted by different authentication schemes.
 *
 * {@link com.amazonaws.mobileconnectors.appsync.client.LatchedGraphQLCallback}
 * and {@link com.amazonaws.mobileconnectors.appsync.client.LatchedSubscriptionCallback}
 * are useful for making async calls into synchronous calls, so that you can assert
 * outcomes after the result is furnished.
 *
 * {@link com.amazonaws.mobileconnectors.appsync.client.NoOpGraphQLCallback}
 * and {@link com.amazonaws.mobileconnectors.appsync.client.DelegatingGraphQLCallback}
 * are utility callbacks to reduce boiler-plate by either doing nothing, or by
 * supplying lambdas instead of a verbose, anonymous instance
 * of {@link com.apollographql.apollo.GraphQLCall.Callback}.
 */
package com.amazonaws.mobileconnectors.appsync.client;
