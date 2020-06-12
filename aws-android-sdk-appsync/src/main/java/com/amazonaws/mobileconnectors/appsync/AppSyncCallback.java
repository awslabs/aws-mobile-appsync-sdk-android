/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.os.Handler;
import android.os.Looper;

import com.amazonaws.apollographql.apollo.GraphQLCall;
import com.amazonaws.apollographql.apollo.api.Response;
import com.amazonaws.apollographql.apollo.exception.ApolloException;
import com.amazonaws.apollographql.apollo.exception.ApolloHttpException;
import com.amazonaws.apollographql.apollo.exception.ApolloNetworkException;
import com.amazonaws.apollographql.apollo.exception.ApolloParseException;

import javax.annotation.Nonnull;

import static com.amazonaws.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * <p>Android wrapper for {@link GraphQLCall.Callback} to be operated on specified {@link Handler}</p>
 *
 * <b>NOTE:</b> {@link #onHttpError(ApolloHttpException)} will be called on the background thread if provided handler is
 * attached to the main looper. This behaviour is intentional as {@link ApolloHttpException} internally has a reference
 * to raw {@link okhttp3.Response} that must be closed on the background, otherwise it throws {@link
 * android.os.NetworkOnMainThreadException} exception.
 */
public final class AppSyncCallback<T> extends GraphQLCall.Callback<T> {
  private final GraphQLCall.Callback<T> delegate;
  private final Handler handler;

  /**
   * Wraps {@code callback} to be be operated on specified {@code handler}
   *
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public static <T> AppSyncCallback<T> wrap(@Nonnull GraphQLCall.Callback<T> callback, @Nonnull Handler handler) {
    return new AppSyncCallback<>(callback, handler);
  }

  /**
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public AppSyncCallback(@Nonnull GraphQLCall.Callback<T> callback, @Nonnull Handler handler) {
    this.delegate = checkNotNull(callback, "callback == null");
    this.handler = checkNotNull(handler, "handler == null");
  }

  @Override
  public void onResponse(@Nonnull final Response<T> response) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        delegate.onResponse(response);
      }
    });
  }

  @Override
  public void onStatusEvent(@Nonnull final GraphQLCall.StatusEvent event) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        delegate.onStatusEvent(event);
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
    if (Looper.getMainLooper() == handler.getLooper()) {
      delegate.onHttpError(e);
    } else {
      handler.post(new Runnable() {
        @Override
        public void run() {
          delegate.onHttpError(e);
        }
      });
    }
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

  @Override
  public void onParseError(@Nonnull final ApolloParseException e) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        delegate.onParseError(e);
      }
    });
  }
}
