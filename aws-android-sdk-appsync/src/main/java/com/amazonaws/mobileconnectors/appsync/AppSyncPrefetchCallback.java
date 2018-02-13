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

package com.amazonaws.mobileconnectors.appsync;

import android.os.Handler;

import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * Android wrapper for {@link AppSyncPrefetch.Callback} to be operated on specified {@link Handler}
 */
public final class AppSyncPrefetchCallback extends AppSyncPrefetch.Callback {
  private final AppSyncPrefetch.Callback delegate;
  private final Handler handler;

  /**
   * Wraps {@code callback} to be be operated on specified {@code handler}
   *
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public static <T> AppSyncPrefetchCallback wrap(@Nonnull AppSyncPrefetch.Callback callback, @Nonnull Handler handler) {
    return new AppSyncPrefetchCallback(callback, handler);
  }

  /**
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public AppSyncPrefetchCallback(@Nonnull AppSyncPrefetch.Callback callback, @Nonnull Handler handler) {
    this.delegate = checkNotNull(callback, "callback == null");
    this.handler = checkNotNull(handler, "handler == null");
  }

  @Override public void onSuccess() {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onSuccess();
      }
    });
  }

  @Override public void onFailure(@Nonnull final ApolloException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onFailure(e);
      }
    });
  }

  @Override public void onHttpError(@Nonnull final ApolloHttpException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onHttpError(e);
      }
    });
  }

  @Override public void onNetworkError(@Nonnull final ApolloNetworkException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onNetworkError(e);
      }
    });
  }
}
