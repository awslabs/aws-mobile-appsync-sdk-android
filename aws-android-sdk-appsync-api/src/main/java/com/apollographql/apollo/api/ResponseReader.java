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

import java.util.List;

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
public interface ResponseReader {

  String readString(ResponseField field);

  Integer readInt(ResponseField field);

  Long readLong(ResponseField field);

  Double readDouble(ResponseField field);

  Boolean readBoolean(ResponseField field);

  <T> T readObject(ResponseField field, ObjectReader<T> objectReader);

  <T> List<T> readList(ResponseField field, ListReader<T> listReader);

  <T> T readCustomType(ResponseField.CustomTypeField field);

  <T> T readConditional(ResponseField field, ConditionalTypeReader<T> conditionalTypeReader);

  interface ObjectReader<T> {
    T read(ResponseReader reader);
  }

  interface ListReader<T> {
    T read(ListItemReader reader);
  }

  interface ConditionalTypeReader<T> {
    T read(String conditionalType, ResponseReader reader);
  }

  interface ListItemReader {

    String readString();

    Integer readInt();

    Long readLong();

    Double readDouble();

    Boolean readBoolean();

    <T> T readCustomType(ScalarType scalarType);

    <T> T readObject(ObjectReader<T> objectReader);

    <T> List<T> readList(ListReader<T> listReader);
  }
}