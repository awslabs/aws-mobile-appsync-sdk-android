/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
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
