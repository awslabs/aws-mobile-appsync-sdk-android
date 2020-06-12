/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.cache.normalized;

import com.amazonaws.apollographql.apollo.api.Operation;
import com.amazonaws.apollographql.apollo.api.ResponseField;

import javax.annotation.Nonnull;

public interface CacheKeyBuilder {
    @Nonnull String build(@Nonnull ResponseField field, @Nonnull Operation.Variables variables);
}
