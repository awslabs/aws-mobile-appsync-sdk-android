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

package com.apollographql.apollo.internal;

import com.apollographql.apollo.Logger;
import com.apollographql.apollo.api.internal.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ApolloLogger {

  private final Optional<Logger> logger;

  public ApolloLogger(@Nonnull Optional<Logger> logger) {
    this.logger = checkNotNull(logger, "logger == null");
  }

  public void d(@Nonnull String message, Object... args) {
    log(Logger.DEBUG, message, null, args);
  }

  public void d(@Nullable Throwable t, @Nonnull String message, Object... args) {
    log(Logger.DEBUG, message, t, args);
  }

  public void w(@Nonnull String message, Object... args) {
    log(Logger.WARN, message, null, args);
  }

  public void w(@Nullable Throwable t, @Nonnull String message, Object... args) {
    log(Logger.WARN, message, t, args);
  }

  public void e(@Nonnull String message, Object... args) {
    log(Logger.ERROR, message, null, args);
  }

  public void e(@Nullable Throwable t, @Nonnull String message, Object... args) {
    log(Logger.ERROR, message, t, args);
  }

  private void log(int priority, @Nonnull String message, @Nullable Throwable t, Object... args) {
    if (logger.isPresent()) {
      logger.get().log(priority, message, Optional.fromNullable(t), args);
    }
  }
}
