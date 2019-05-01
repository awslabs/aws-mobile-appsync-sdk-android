/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal.cache.normalized;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess") public interface Transaction<T, R> {
  @Nullable R execute(T cache);
}
