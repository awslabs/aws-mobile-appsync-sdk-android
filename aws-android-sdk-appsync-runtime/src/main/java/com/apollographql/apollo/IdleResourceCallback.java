/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo;

/**
 * Callback which gets invoked when the resource transitions
 * from active to idle state.
 */
public interface IdleResourceCallback {

  /**
   * Gets called when the resource transitions from active to idle state.
   */
  void onIdle();

}
