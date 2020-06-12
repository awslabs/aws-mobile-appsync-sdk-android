/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api;

import javax.annotation.Nullable;

public final class Input<V> {
  public final V value;
  public final boolean defined;

  private Input(V value, boolean defined) {
    this.value = value;
    this.defined = defined;
  }

  public static <V> Input<V> fromNullable(@Nullable V value) {
    return new Input<>(value, true);
  }

  public static <V> Input<V> absent() {
    return new Input<>(null, false);
  }
}
