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
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.cache.CacheHeaders;

import javax.annotation.Nonnull;

/**
 * A call prepared to execute GraphQL mutation operation.
 */
public interface AppSyncMutationCall<T> extends GraphQLCall<T> {

  /**
   * <p>Sets a list of {@link AppSyncQueryWatcher} query names to be re-fetched once this mutation completed.</p>
   *
   * @param operationNames array of {@link OperationName} query names to be re-fetched
   * @return {@link AppSyncMutationCall} that will trigger re-fetching provided queries
   */
  @Nonnull
  AppSyncMutationCall<T> refetchQueries(@Nonnull OperationName... operationNames);

  /**
   * <p>Sets a list of {@link Query} to be re-fetched once this mutation completed.</p>
   *
   * @param queries array of {@link Query} to be re-fetched
   * @return {@link AppSyncMutationCall} that will trigger re-fetching provided queries
   */
  @Nonnull
  AppSyncMutationCall<T> refetchQueries(@Nonnull Query... queries);

  @Nonnull @Override
  AppSyncMutationCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders);

  @Nonnull @Override
  AppSyncMutationCall<T> clone();

  /**
   * Factory for creating {@link AppSyncMutationCall} calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new {@link AppSyncMutationCall} call.
     *
     * @param mutation the {@link Mutation} which needs to be performed
     * @return prepared {@link AppSyncMutationCall} call to be executed at some point in the future
     */
    <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(
        @Nonnull Mutation<D, T, V> mutation);

    /**
     * <p>Creates and prepares a new {@link AppSyncMutationCall} call with optimistic updates.</p>
     *
     * Provided optimistic updates will be stored in {@link ApolloStore}
     * immediately before mutation execution. Any {@link AppSyncQueryWatcher} dependent on the changed cache records will
     * be re-fetched.
     *
     * @param mutation              the {@link Mutation} which needs to be performed
     * @param withOptimisticUpdates optimistic updates for this mutation
     * @return prepared {@link AppSyncMutationCall} call to be executed at some point in the future
     */
    <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(
        @Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates);
  }
}
