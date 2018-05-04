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

import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import okhttp3.Response;

/**
 * <p>AppSyncPrefetch is an abstraction for a request that has been prepared for execution. It represents a single
 * request/response pair and cannot be executed twice, though it can be cancelled. It fetches the graph response from
 * the server on successful completion but <b>doesn't</b> inflate the response into models. Instead it stores the raw
 * response in the request/response cache and defers the parsing to a later time.</p>
 *
 *
 * <p>Use this object for use cases when the data needs to be fetched, but is not required for immediate consumption.
 * e.g.background update/syncing.</p>
 *
 * <p>Note: In order to execute the request again, call the {@link AppSyncPrefetch#clone()} method which creates a new
 * {@link AppSyncPrefetch} object.</p>
 */
public interface AppSyncPrefetch extends Cancelable {
  /**
   * Schedules the request to be executed at some point in the future.
   *
   * @param callback Callback which will handle the success response or a failure exception
   * @throws IllegalStateException when the call has already been executed
   */
   void enqueue(@Nullable Callback callback);

  /**
   * Creates a new, identical AppSyncPrefetch to this one which can be enqueued or executed even if this one has already
   * been executed.
   *
   * @return The cloned AppSyncPrefetch object
   */
  AppSyncPrefetch clone();

  /**
   * Returns GraphQL operation this call executes
   *
   * @return {@link Operation}
   */
  @Nonnull Operation operation();

  /**
   * Cancels this {@link AppSyncPrefetch}. If the call has already completed, nothing will happen.
   * If the call is outgoing, an {@link ApolloCanceledException} will be thrown if the call was started
   * with {@link #execute()}. If the call was started with {@link #enqueue(Callback)}
   * the {@link Callback} will be disposed, and will receive no more events.
   * The call will attempt to abort and release resources, if possible.
   */
  @Override void cancel();

  /**
   * Communicates responses from the server.
   */
  abstract class Callback {

    /**
     * Gets called when the request has succeeded.
     */
    public abstract void onSuccess();

    /**
     * Gets called when an unexpected exception occurs while creating the request or processing the response.
     */
    public abstract void onFailure(@Nonnull ApolloException e);

    /**
     * Gets called when an http request error takes place. This is the case when the returned http status code doesn't
     * lie in the range 200 (inclusive) and 300 (exclusive).
     */
    public void onHttpError(@Nonnull ApolloHttpException e) {
      onFailure(e);
      Response response = e.rawResponse();
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
     * Gets called when {@link GraphQLCall} has been canceled.
     */
    public void onCanceledError(@Nonnull ApolloCanceledException e) {
      onFailure(e);
    }
  }

  /**
   * Factory for creating AppSyncPrefetch object.
   */
  interface Factory {

    /**
     * Creates the AppSyncPrefetch by wrapping the operation object inside.
     *
     * @param operation the operation which needs to be performed
     * @return The AppSyncPrefetch object with the wrapped operation object
     */
    <D extends Operation.Data, T, V extends Operation.Variables> AppSyncPrefetch prefetch(
            @Nonnull Operation<D, T, V> operation);

  }
}
