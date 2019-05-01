/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
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
