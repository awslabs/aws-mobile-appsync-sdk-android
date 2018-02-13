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

package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.Record;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class RecordWeigher {

  private static final int SIZE_OF_BOOLEAN = 16;
  private static final int SIZE_OF_BIG_DECIMAL = 32;
  private static final int SIZE_OF_ARRAY_OVERHEAD = 16;
  private static final int SIZE_OF_RECORD_OVERHEAD = 16;
  private static final int SIZE_OF_CACHE_REFERENCE_OVERHEAD = 16;
  private static final int SIZE_OF_NULL = 4;

  public static int byteChange(Object newValue, Object oldValue) {
    return weighField(newValue) - weighField(oldValue);
  }

  public static int calculateBytes(Record record) {
    int size = SIZE_OF_RECORD_OVERHEAD + record.key().getBytes().length;
    for (Map.Entry<String, Object> field : record.fields().entrySet()) {
      size += (field.getKey().getBytes().length + weighField(field.getValue()));
    }
    return size;
  }

  private static int weighField(Object field) {
    if (field instanceof List) {
      int size = SIZE_OF_ARRAY_OVERHEAD;
      for (Object listItem : (List) field) {
        size += weighField(listItem);
      }
      return size;
    }
    if (field instanceof String) {
      return ((String) field).getBytes().length;
    } else if (field instanceof Boolean) {
      return SIZE_OF_BOOLEAN;
    } else if (field instanceof BigDecimal) {
      return SIZE_OF_BIG_DECIMAL;
    } else if (field instanceof CacheReference) {
      return SIZE_OF_CACHE_REFERENCE_OVERHEAD + ((CacheReference) field).key().getBytes().length;
    } else if (field == null) {
      return SIZE_OF_NULL;
    }
    throw new IllegalStateException("Unknown field type in Record. " + field.getClass().getName());
  }

}
