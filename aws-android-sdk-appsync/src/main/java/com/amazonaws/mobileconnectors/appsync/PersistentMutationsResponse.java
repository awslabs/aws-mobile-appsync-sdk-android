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
