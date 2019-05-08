/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PersistentMutationsResponse.
 */

public class PersistentMutationsResponse {
    private JSONObject data;
    private JSONArray errors;
    private String mutationClassName;
    private String recordIdentifier;

    public PersistentMutationsResponse(JSONObject data, JSONArray errors, String mutationClassName, String recordIdentifier) {
        this.data = data;
        this.errors = errors;
        this.mutationClassName = mutationClassName;
        this.recordIdentifier = recordIdentifier;
    }

    public JSONObject getDataJSONObject() {
        return this.data;
    }

    public JSONArray getErrorsJSONObject() {
        return this.errors;
    }

    public String getMutationClassName() {
        return this.mutationClassName;
    }

    public String getRecordIdentifier() {
        return this.recordIdentifier;
    }
}
