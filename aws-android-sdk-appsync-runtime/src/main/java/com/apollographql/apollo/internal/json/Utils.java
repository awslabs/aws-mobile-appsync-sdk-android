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

package com.apollographql.apollo.internal.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import okio.Buffer;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class Utils {

    public static String toJsonString(@Nonnull Object data) throws IOException {
        checkNotNull(data, "data == null");

        Buffer buffer = new Buffer();
        JsonWriter jsonWriter = JsonWriter.of(buffer);
        writeToJson(data, jsonWriter);
        jsonWriter.close();

        return buffer.readUtf8();
    }

    @SuppressWarnings("unchecked")
    public static void writeToJson(Object value, JsonWriter jsonWriter) throws IOException {
        if (value == null) {
            jsonWriter.nullValue();
        } else if (value instanceof Map) {
            jsonWriter.beginObject();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                String key = entry.getKey().toString();
                jsonWriter.name(key);
                writeToJson(entry.getValue(), jsonWriter);
            }
            jsonWriter.endObject();
        } else if (value instanceof List) {
            jsonWriter.beginArray();
            for (Object item : (List) value) {
                writeToJson(item, jsonWriter);
            }
            jsonWriter.endArray();
        } else if (value instanceof Boolean) {
            jsonWriter.value((Boolean) value);
        } else if (value instanceof Number) {
            jsonWriter.value((Number) value);
        } else {
            jsonWriter.value(value.toString());
        }
    }

    private Utils() {
    }
}
