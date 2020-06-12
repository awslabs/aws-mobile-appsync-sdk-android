/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api;

/**
 * Represents a GraphQL subscription.
 */
public interface Subscription<D extends Operation.Data, T, V extends Operation.Variables> extends Operation<D, T, V> {
}
