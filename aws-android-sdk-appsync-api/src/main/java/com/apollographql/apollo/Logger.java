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

package com.apollographql.apollo;

import com.apollographql.apollo.api.internal.Optional;

import javax.annotation.Nonnull;

/**
 * Logger to use for logging
 */
public interface Logger {
  int DEBUG = 3;
  int WARN = 5;
  int ERROR = 6;

  /**
   * Logs the message to the appropriate channel (file, console, etc)
   *
   * @param priority the priority to set
   * @param message message to log
   * @param t Optional throwable to log
   * @param args extra arguments to pass to the logged message.
   */
  void log(int priority, @Nonnull String message, @Nonnull Optional<Throwable> t, @Nonnull Object... args);
}
