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

package com.apollographql.apollo.internal.response;

import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;

import java.util.List;

public interface ResponseReaderShadow<R> {

  void willResolveRootQuery(Operation operation);

  void willResolve(ResponseField field, Operation.Variables variables);

  void didResolve(ResponseField field, Operation.Variables variables);

  void didResolveScalar(Object value);

  void willResolveObject(ResponseField objectField, Optional<R> objectSource);

  void didResolveObject(ResponseField objectField, Optional<R> objectSource);

  void didResolveList(List array);

  void willResolveElement(int atIndex);

  void didResolveElement(int atIndex);

  void didResolveNull();
}
