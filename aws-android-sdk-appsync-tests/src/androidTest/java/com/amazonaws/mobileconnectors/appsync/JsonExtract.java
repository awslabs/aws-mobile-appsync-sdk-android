/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import org.json.JSONException;
import org.json.JSONObject;

final class JsonExtract {
    private JsonExtract() {}

    // This is a poor man's implementation of JSONPath, that just handles literals,
    // with the '.' meaning "down one more level." This will break if your key contains a period.
    static String stringValue(JSONObject container, String path) {
        int indexOfFirstPeriod = path.indexOf(".");
        if (indexOfFirstPeriod != -1) {
            String firstPortion = path.substring(0, indexOfFirstPeriod);
            String rest = path.substring(indexOfFirstPeriod + 1);

            try {
                return stringValue(container.getJSONObject(firstPortion), rest);
            } catch (JSONException e) {
                throw new RuntimeException("could not find " + path);
            }
        }
        try {
            return container.getString(path);
        } catch (JSONException jsonException) {
            throw new RuntimeException(
                "Failed to get key " + path + " from " + container +
                    ", please check that it is correctly formed.", jsonException
            );
        }
    }
}
