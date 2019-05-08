/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal.json;

import com.apollographql.apollo.cache.normalized.CacheReference;

import java.io.IOException;

/**
 * A {@link ResponseJsonStreamReader} with additional support for {@link CacheReference}.
 */
public final class CacheJsonStreamReader extends ResponseJsonStreamReader {

  public CacheJsonStreamReader(JsonReader jsonReader) {
    super(jsonReader);
  }

  @Override public Object nextScalar(boolean optional) throws IOException {
    Object scalar = super.nextScalar(optional);
    if (scalar instanceof String) {
      String scalarString = (String) scalar;
      if (CacheReference.canDeserialize(scalarString)) {
        return CacheReference.deserialize(scalarString);
      }
    }
    return scalar;
  }

}
