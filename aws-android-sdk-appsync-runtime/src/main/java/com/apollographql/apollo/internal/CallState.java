/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal;

enum CallState {
  IDLE, ACTIVE, TERMINATED, CANCELED;

  static class IllegalStateMessage {
    private final CallState callState;

    private IllegalStateMessage(CallState callState) {
      this.callState = callState;
    }

    static IllegalStateMessage forCurrentState(CallState callState) {
      return new IllegalStateMessage(callState);
    }

    String expected(CallState... acceptableStates) {
      StringBuilder stringBuilder = new StringBuilder("Expected: " + callState.name() + ", but found [");
      String deliminator = "";
      for (CallState state : acceptableStates) {
        stringBuilder.append(deliminator).append(state.name());
        deliminator = ", ";
      }
      return stringBuilder.append("]").toString();
    }
  }
}
