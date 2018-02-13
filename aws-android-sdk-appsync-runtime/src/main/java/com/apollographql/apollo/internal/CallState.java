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
