/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnmodifiableMapBuilder<K, V> {

  private final Map<K, V> map;

  public UnmodifiableMapBuilder(int initialCapacity) {
    this.map = new HashMap<>(initialCapacity);
  }

  public UnmodifiableMapBuilder() {
    this.map = new HashMap<>();
  }

  public UnmodifiableMapBuilder<K, V> put(K key, V value) {
    map.put(key, value);
    return this;
  }

  public Map<K, V> build() {
    return Collections.unmodifiableMap(map);
  }

}
