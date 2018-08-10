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

package com.amazonaws.mobileconnectors.appsync.sigv4;

import android.util.Log;

import com.amazonaws.mobile.config.AWSConfiguration;

public class BasicAPIKeyAuthProvider implements APIKeyAuthProvider {
    private final String apiKey;

    private static final String TAG = BasicAPIKeyAuthProvider.class.getSimpleName();

    public BasicAPIKeyAuthProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Read the ApiKey from AppSync section of the awsconfiguration.json file.
     *
     * <p>
     * "AppSync": {
     *   "Default": {
     *       "ApiUrl": "https://xxxxxxxxxxxxxxxx.appsync-api.<region>.amazonaws.com/graphql",
     *       "Region": "us-east-1",
     *       "ApiKey": "da2-yyyyyyyyyyyyyyyy",
     *       "AuthMode": "API_KEY"
     *   }
     * }
     * </p>
     *
     * <p>
     *      AWSConfiguration awsConfiguration = new AWSConfiguration(getApplicationContext();
     *      APIKeyAuthProvider apiKeyAuthProvider = new BasicAPIKeyAuthProvider(awsConfiguration);
     * </p>
     *
     * @param awsConfiguration The object representing the configuration
     *                         information from awsconfiguration.json
     */
    public BasicAPIKeyAuthProvider(AWSConfiguration awsConfiguration) {
        try {
            this.apiKey = awsConfiguration.optJsonObject("AppSync").getString("ApiKey");
        } catch (Exception exception) {
            Log.e(TAG, "Please check the ApiKey passed from awsconfiguration.json.");
            throw new RuntimeException("Please check the ApiKey passed from awsconfiguration.json.", exception);
        }
    }

    @Override
    public String getAPIKey() {
        return apiKey;
    }
}
