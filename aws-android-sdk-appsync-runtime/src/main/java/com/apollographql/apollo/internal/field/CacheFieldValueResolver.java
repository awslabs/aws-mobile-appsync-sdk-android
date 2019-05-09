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

package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.cache.normalized.ReadableStore;

import java.util.ArrayList;
import java.util.List;

public final class CacheFieldValueResolver implements FieldValueResolver<Record> {
  private final ReadableStore readableCache;
  private final Operation.Variables variables;
  private final CacheKeyResolver cacheKeyResolver;
  private final CacheHeaders cacheHeaders;

  public CacheFieldValueResolver(ReadableStore readableCache, Operation.Variables variables,
      CacheKeyResolver cacheKeyResolver, CacheHeaders cacheHeaders) {
    this.readableCache = readableCache;
    this.variables = variables;
    this.cacheKeyResolver = cacheKeyResolver;
    this.cacheHeaders = cacheHeaders;
  }

  @SuppressWarnings("unchecked") @Override public <T> T valueFor(Record record, ResponseField field) {
    switch (field.type()) {
      case OBJECT:
        return (T) valueForObject(record, field);

      case LIST: {
        return (T) valueForList((List) fieldValue(record, field));
      }

      default:
        return fieldValue(record, field);
    }
  }

  private Record valueForObject(Record record, ResponseField field) {
    CacheReference cacheReference;
    CacheKey fieldCacheKey = cacheKeyResolver.fromFieldArguments(field, variables);
    if (fieldCacheKey != CacheKey.NO_KEY) {
      cacheReference = new CacheReference(fieldCacheKey.key());
    } else {
      cacheReference = fieldValue(record, field);
    }

    if (cacheReference != null) {
      Record referencedRecord = readableCache.read(cacheReference.key(), cacheHeaders);
      if (referencedRecord == null) {
        // we are unable to find record in the cache by reference,
        // means it was removed intentionally by using imperative store API or
        // evicted from LRU cache, we must prevent of further resolving cache response as it's broken
        throw new IllegalStateException("Cache MISS: failed to find record in cache by reference");
      }
      return referencedRecord;
    }

    return null;
  }

  @SuppressWarnings("unchecked") private List valueForList(List values) {
    if (values == null) {
      return null;
    }

    List result = new ArrayList();
    for (Object value : values) {
      if (value instanceof CacheReference) {
        CacheReference reference = (CacheReference) value;
        Record referencedRecord = readableCache.read(reference.key(), cacheHeaders);
        if (referencedRecord == null) {
          // we are unable to find record in the cache by reference,
          // means it was removed intentionally by using imperative store API or
          // evicted from LRU cache, we must prevent of further resolving cache response as it's broken
          throw new IllegalStateException("Cache MISS: failed to find record in cache by reference");
        }
        result.add(referencedRecord);
      } else if (value instanceof List) {
        result.add(valueForList((List) value));
      } else {
        result.add(value);
      }
    }
    return result;
  }


  @SuppressWarnings("unchecked") private <T> T fieldValue(Record record, ResponseField field) {
    String fieldKey = field.cacheKey(variables);
    if (!record.hasField(fieldKey)) {
      return null;
    }
    return (T) record.field(fieldKey);
  }
}
