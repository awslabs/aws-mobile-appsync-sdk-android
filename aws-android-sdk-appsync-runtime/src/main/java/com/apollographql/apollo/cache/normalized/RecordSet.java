/**
 * Copyright 2018-2019 Amazon.com,
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

package com.apollographql.apollo.cache.normalized;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RecordSet {

  private final Map<String, Record> recordMap = new LinkedHashMap<>();

  public Record get(String key) {
    return recordMap.get(key);
  }

  public Set<String> merge(Record apolloRecord) {
    final Record oldRecord = recordMap.get(apolloRecord.key());
    if (oldRecord == null) {
      recordMap.put(apolloRecord.key(), apolloRecord);
      return Collections.emptySet();
    } else {
      return oldRecord.mergeWith(apolloRecord);
    }
  }

  public Collection<Record> allRecords() {
    return recordMap.values();
  }
}
