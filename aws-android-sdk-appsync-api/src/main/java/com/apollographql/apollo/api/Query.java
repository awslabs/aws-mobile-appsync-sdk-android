/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.api;

/**
 * Represents a GraphQL query that will be sent to the server.
 */
public interface Query<D extends Operation.Data, T, V extends Operation.Variables> extends Operation<D, T, V> {
}
