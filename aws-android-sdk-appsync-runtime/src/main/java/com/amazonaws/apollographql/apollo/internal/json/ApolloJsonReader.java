/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.internal.json;

import okio.BufferedSource;

public final class ApolloJsonReader {

  public static CacheJsonStreamReader cacheJsonStreamReader(BufferedSourceJsonReader sourceJsonReader) {
    return new CacheJsonStreamReader(sourceJsonReader);
  }

  public static ResponseJsonStreamReader responseJsonStreamReader(BufferedSourceJsonReader sourceJsonReader) {
    return new ResponseJsonStreamReader(sourceJsonReader);
  }

  public static BufferedSourceJsonReader bufferedSourceJsonReader(BufferedSource bufferedSource) {
    return new BufferedSourceJsonReader(bufferedSource);
  }
}
