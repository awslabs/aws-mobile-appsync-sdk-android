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

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseFieldMapper;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ResponseFieldMapperFactory {
  private final ConcurrentHashMap<Class, ResponseFieldMapper> pool = new ConcurrentHashMap<>();

  @Nonnull public ResponseFieldMapper create(@Nonnull Operation operation) {
    checkNotNull(operation, "operation == null");
    Class operationClass = operation.getClass();
    ResponseFieldMapper mapper = pool.get(operationClass);
    if (mapper != null) {
      return mapper;
    }
    pool.putIfAbsent(operationClass, operation.responseFieldMapper());
    return pool.get(operationClass);
  }
}
