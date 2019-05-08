/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo;

import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.GraphQLCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p>GraphQLCall is an abstraction for a request that has been prepared for execution. GraphQLCall represents a single
 * request/response pair and cannot be executed twice, though it can be cancelled.</p>
 *
 * <p>In order to execute the request again, call the {@link GraphQLCall#clone()} method which creates a new GraphQLCall
 * object.</p>
 */
public interface GraphQLCall<T> extends Cancelable {
  /**
   * Schedules the request to be executed at some point in the future.
   *
   * @param callback Callback which will handle the response or a failure exception.
   * @throws IllegalStateException when the call has already been executed
   */
  void enqueue(@Nullable Callback<T> callback);

  /**
   * Sets the {@link CacheHeaders} to use for this call. {@link com.apollographql.apollo.interceptor.FetchOptions} will
   * be configured with this headers, and will be accessible from the {@link ResponseFetcher} used for this call.
   *
   * @param cacheHeaders the {@link CacheHeaders} that will be passed with records generated from this request to {@link
   *                     NormalizedCache}. Standardized cache headers are
   *                     defined in {@link GraphQLCacheHeaders}.
   * @return The GraphQLCall object with the provided {@link CacheHeaders}.
   */
  @Nonnull
  GraphQLCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders);

  /**
   * Creates a new, identical call to this one which can be enqueued or executed even if this call has already been.
   *
   * @return The cloned GraphQLCall object.
   */
  @Nonnull
  GraphQLCall<T> clone();

  /**
   * Returns GraphQL operation this call executes
   *
   * @return {@link Operation}
   */
  @Nonnull Operation operation();

  /**
   * Cancels this {@link GraphQLCall}. If the call has already completed, nothing will happen.
   * If the call is outgoing, an {@link ApolloCanceledException} will be thrown if the call was started
   * with {@link #execute()}. If the call was started with {@link #enqueue(Callback)}
   * the {@link Callback} will be disposed, and will receive no more events. The
   * call will attempt to abort and release resources, if possible.
   */
  @Override void cancel();

  /**
   * Communicates responses from a server or offline requests.
   */
  abstract class Callback<T> {

    /**
     * Gets called when GraphQL response is received and parsed successfully. Depending on the
     * {@link ResponseFetcher} used with the call, this may be called multiple times. {@link #onCompleted()}
     * will be called after the final call to onResponse.
     *
     * @param response the GraphQL response
     */
    public abstract void onResponse(@Nonnull Response<T> response);

    /**
     * Gets called when an unexpected exception occurs while creating the request or processing the response.
     * Will be called at most one time. It is considered a terminal event. After called,
     * neither {@link #onResponse(Response)} or {@link #onCompleted()} will be called again.
     */
    public abstract void onFailure(@Nonnull ApolloException e);

    /**
     * Gets called whenever any action happen to this {@link GraphQLCall}.
     *
     * @param event status that corresponds to a {@link GraphQLCall} action
     */
    public void onStatusEvent(@Nonnull StatusEvent event) { }

    /**
     * <p>Gets called when an http request error takes place. This is the case when the returned http status code
     * doesn't lie in the range 200 (inclusive) and 300 (exclusive).</p>
     *
     * <b>NOTE:</b> by overriding this callback you must call {@link okhttp3.Response#close()} on {@link
     * ApolloHttpException#rawResponse} to close the network connection.
     */
    public void onHttpError(@Nonnull ApolloHttpException e) {
      onFailure(e);
      okhttp3.Response response = e.rawResponse();
      if (response != null) {
        response.close();
      }
    }

    /**
     * Gets called when an http request error takes place due to network failures, timeouts etc.
     */
    public void onNetworkError(@Nonnull ApolloNetworkException e) {
      onFailure(e);
    }

    /**
     * Gets called when the network request succeeds but there was an error parsing the response.
     */
    public void onParseError(@Nonnull ApolloParseException e) {
      onFailure(e);
    }

    /**
     * Gets called when {@link GraphQLCall} has been canceled.
     */
    public void onCanceledError(@Nonnull ApolloCanceledException e) {
      onFailure(e);
    }
  }

  /**
   * Represents a status event that corresponds to a {@link GraphQLCall} action
   */
  enum StatusEvent {
    /**
     * {@link GraphQLCall} is scheduled for execution
     */
    SCHEDULED,
    /**
     * {@link GraphQLCall} fetches response from cache
     */
    FETCH_CACHE,
    /**
     * {@link GraphQLCall} fetches response from network
     */
    FETCH_NETWORK,
    /**
     * {@link GraphQLCall} is finished its execution
     */
    COMPLETED
  }
}
