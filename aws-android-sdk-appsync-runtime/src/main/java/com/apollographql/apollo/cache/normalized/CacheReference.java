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

package com.apollographql.apollo.cache.normalized;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CacheReference {

  private final String key;
  private static final Pattern SERIALIZATION_REGEX_PATTERN = Pattern.compile("ApolloCacheReference\\{(.*)\\}");
  private static final String SERIALIZATION_TEMPLATE = "ApolloCacheReference{%s}";

  public CacheReference(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CacheReference that = (CacheReference) o;

    return key != null ? key.equals(that.key) : that.key == null;

  }

  @Override public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  @Override public String toString() {
    return key;
  }

  public String serialize() {
    return String.format(SERIALIZATION_TEMPLATE, key);
  }

  public static CacheReference deserialize(String serializedCacheReference) {
    Matcher matcher = SERIALIZATION_REGEX_PATTERN.matcher(serializedCacheReference);
    if (!matcher.find() || matcher.groupCount() != 1) {
      throw new IllegalArgumentException("Not a cache reference: " + serializedCacheReference
          + " Must be of the form:" + SERIALIZATION_TEMPLATE);
    }
    return new CacheReference(matcher.group(1));
  }

  public static boolean canDeserialize(String value) {
    Matcher matcher = SERIALIZATION_REGEX_PATTERN.matcher(value);
    return matcher.matches();
  }

}

