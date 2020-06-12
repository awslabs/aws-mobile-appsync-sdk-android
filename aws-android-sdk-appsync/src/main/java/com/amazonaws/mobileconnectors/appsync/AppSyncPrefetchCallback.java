/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.os.Handler;

import com.amazonaws.apollographql.apollo.exception.ApolloException;
import com.amazonaws.apollographql.apollo.exception.ApolloHttpException;
import com.amazonaws.apollographql.apollo.exception.ApolloNetworkException;

import javax.annotation.Nonnull;

import static com.amazonaws.apollographql.apollo.api.internal.Utils.checkNotNull;

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

  @Override
  public void onSuccess() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        delegate.onSuccess();
      }
    });
  }

  @Override
  public void onFailure(@Nonnull final ApolloException e) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        delegate.onFailure(e);
      }
    });
  }

  @Override
  public void onHttpError(@Nonnull final ApolloHttpException e) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        delegate.onHttpError(e);
      }
    });
  }

  @Override
  public void onNetworkError(@Nonnull final ApolloNetworkException e) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        delegate.onNetworkError(e);
      }
    });
  }
}
