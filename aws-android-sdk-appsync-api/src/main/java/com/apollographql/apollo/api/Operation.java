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

package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Represents a GraphQL operation (mutation or query).
 */
public interface Operation<D extends Operation.Data, T, V extends Operation.Variables> {
  /**
   * Returns the raw GraphQL operation String.
   */
  String queryDocument();

  /**
   * Returns the variables associated with this GraphQL operation.
   */
  V variables();

  /**
   * Returns a mapper that maps the server response data to generated model class {@link D}.
   */
  ResponseFieldMapper<D> responseFieldMapper();

  /**
   * Wraps the generated response data class {@link D} with another class. For example, a use case for this would be to
   * wrap the generated response data class in an Optional i.e. Optional.fromNullable(data).
   */
  T wrapData(D data);

  /**
   * Returns GraphQL operation name.
   *
   * @return {@link OperationName} operation name
   */
  @Nonnull
  OperationName name();

  /**
   * Returns a unique identifier for this operation.
   *
   * @return operation identifier.
   */
  @Nonnull String operationId();

  /**
   * Abstraction for data returned by the server in response to this operation.
   */
  interface Data {

    /**
     * Returns marshaller to serialize operation data
     *
     * @return {@link ResponseFieldMarshaller} to serialize operation data
     */
    ResponseFieldMarshaller marshaller();
  }

  /**
   * Abstraction for the variables which are a part of the GraphQL operation. For example, for the following GraphQL
   * operation, Variables represents values for GraphQL '$type' and '$limit' variables:
   * <pre>{@code
   *      query FeedQuery($type: FeedType!, $limit: Int!) {
   *          feedEntries: feed(type: $type, limit: $limit) {
   *          id
   *          repository {
   *              ...RepositoryFragment
   *          }
   *          postedBy {
   *            login
   *          }
   *      }
   *    }
   * }
   * </pre>
   */
  class Variables {
    protected Variables() {
    }

    @Nonnull public Map<String, Object> valueMap() {
      return Collections.emptyMap();
    }

    @Nonnull public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override public void marshal(InputFieldWriter writer) {
        }
      };
    }
  }

  Variables EMPTY_VARIABLES = new Variables();
}
