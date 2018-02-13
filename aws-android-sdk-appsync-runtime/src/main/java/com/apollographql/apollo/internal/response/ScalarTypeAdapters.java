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

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.ScalarType;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ScalarTypeAdapters {
  private static final Map<Class, CustomTypeAdapter> DEFAULT_ADAPTERS = defaultAdapters();
  private final Map<ScalarType, CustomTypeAdapter> customAdapters;

  public ScalarTypeAdapters(@Nonnull Map<ScalarType, CustomTypeAdapter> customAdapters) {
    this.customAdapters = checkNotNull(customAdapters, "customAdapters == null");
  }

  @SuppressWarnings("unchecked") @Nonnull public <T> CustomTypeAdapter<T> adapterFor(@Nonnull ScalarType scalarType) {
    checkNotNull(scalarType, "scalarType == null");

    CustomTypeAdapter<T> customTypeAdapter = customAdapters.get(scalarType);
    if (customTypeAdapter == null) {
      customTypeAdapter = DEFAULT_ADAPTERS.get(scalarType.javaType());
    }

    if (customTypeAdapter == null) {
      throw new IllegalArgumentException(String.format("Can't map GraphQL type: %s to: %s. Did you forget to add "
          + "custom type adapter?", scalarType.typeName(), scalarType.javaType()));
    }

    return customTypeAdapter;
  }

  private static Map<Class, CustomTypeAdapter> defaultAdapters() {
    Map<Class, CustomTypeAdapter> adapters = new LinkedHashMap<>();
    adapters.put(String.class, new DefaultCustomTypeAdapter<String>() {
      @Nonnull @Override public String decode(@Nonnull String value) {
        return value;
      }
    });
    adapters.put(Boolean.class, new DefaultCustomTypeAdapter<Boolean>() {
      @Nonnull @Override public Boolean decode(@Nonnull String value) {
        return Boolean.parseBoolean(value);
      }
    });
    adapters.put(Integer.class, new DefaultCustomTypeAdapter<Integer>() {
      @Nonnull @Override public Integer decode(@Nonnull String value) {
        return Integer.parseInt(value);
      }
    });
    adapters.put(Long.class, new DefaultCustomTypeAdapter<Long>() {
      @Nonnull @Override public Long decode(@Nonnull String value) {
        return Long.parseLong(value);
      }
    });
    adapters.put(Float.class, new DefaultCustomTypeAdapter<Float>() {
      @Nonnull @Override public Float decode(@Nonnull String value) {
        return Float.parseFloat(value);
      }
    });
    adapters.put(Double.class, new DefaultCustomTypeAdapter<Double>() {
      @Nonnull @Override public Double decode(@Nonnull String value) {
        return Double.parseDouble(value);
      }
    });
    return adapters;
  }

  private abstract static class DefaultCustomTypeAdapter<T> implements CustomTypeAdapter<T> {
    @Nonnull @Override public String encode(@Nonnull T value) {
      return value.toString();
    }
  }
}
