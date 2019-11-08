/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.sigv4.AppSyncV4Signer;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicCognitoUserPoolsAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.OidcAuthProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.api.Subscription;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Create one per {@link AWSAppSyncClient}.
 */
class SubscriptionAuthorizer {
    private final AWSConfiguration awsConfiguration;
    private final Context applicationContext;
    private final OidcAuthProvider mOidcAuthProvider;

    SubscriptionAuthorizer(
            AWSConfiguration awsConfiguration,
            OidcAuthProvider mOidcAuthProvider,
            Context applicationContext) {
        this.awsConfiguration = awsConfiguration;
        this.applicationContext = applicationContext;
        this.mOidcAuthProvider = mOidcAuthProvider;
    }

    JSONObject getConnectionAuthorizationDetails() throws JSONException {
        return getAuthorizationDetails(true, null);
    }

    /**
     * Return authorization json to be used for connection and subscription registration.
     */
     JSONObject getAuthorizationDetails(boolean connectionFlag,
                                        Subscription subscription) throws JSONException {
        // Get the Auth Mode from configuration json
        String authMode;
        try {
            authMode = awsConfiguration.optJsonObject("AppSync").getString("AuthMode");
        } catch (JSONException e) {
            throw new RuntimeException("Failed to read AuthMode from awsconfiguration.json", e);
        }

        // Construct the Json based on the Auth Mode
        switch (authMode) {
            case "API_KEY" :
                return getAuthorizationDetailsForApiKey(awsConfiguration);
            case "AWS_IAM" :
                return getAuthorizationDetailsForIAM(connectionFlag, awsConfiguration,
                        subscription, applicationContext);
            case "AMAZON_COGNITO_USER_POOLS" :
                return getAuthorizationDetailsForUserpools(awsConfiguration, applicationContext);
            case "OPENID_CONNECT" :
                return getAuthorizationDetailsForOidc(awsConfiguration, mOidcAuthProvider);
            default :
                throw new RuntimeException("Invalid AuthMode read from awsconfiguration.json.");
        }
    }

    private static JSONObject getAuthorizationDetailsForApiKey(AWSConfiguration awsConfiguration) {
        try {
            return new JSONObject()
                .put("host", getHost(getApiUrl(awsConfiguration)))
                .put("x-amz-date", ISO8601Timestamp.now())
                .put("x-api-key", awsConfiguration.optJsonObject("AppSync").getString("ApiKey"));
        } catch (JSONException | MalformedURLException e) {
            throw new RuntimeException("Error constructing the authorization json for Api key. ", e);
        }
    }

    private static JSONObject getAuthorizationDetailsForIAM(boolean connectionFlag, AWSConfiguration awsConfiguration,
                                                            Subscription subscription,
                                                            Context applicationContext) throws JSONException {
        String identityPoolId;
        String regionStr;
        try {
            JSONObject identityPoolJSON = awsConfiguration.optJsonObject("CredentialsProvider")
                .getJSONObject("CognitoIdentity")
                .getJSONObject(awsConfiguration.getConfiguration());
            identityPoolId = identityPoolJSON.getString("PoolId");
            regionStr = identityPoolJSON.getString("Region");
        } catch (JSONException e) {
            throw new RuntimeException("Error reading identity pool information from awsconfiguration.json", e);
        }

        DefaultRequest canonicalRequest = new DefaultRequest("appsync");

        URI apiUrl;
        try {
            final String baseUrl = getApiUrl(awsConfiguration);
            final String connectionUrl = connectionFlag ? baseUrl + "/connect" : baseUrl;
            apiUrl = new URI(connectionUrl);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error constructing canonical URI for IAM request signature", e);
        }
        canonicalRequest.setEndpoint(apiUrl);

        canonicalRequest.addHeader("accept", "application/json, text/javascript");
        canonicalRequest.addHeader("content-encoding", "amz-1.0");
        canonicalRequest.addHeader("content-type", "application/json; charset=UTF-8");
        canonicalRequest.setHttpMethod(HttpMethodName.valueOf("POST"));

        if (connectionFlag) {
            canonicalRequest.setContent(new ByteArrayInputStream("{}".getBytes()));
        } else {
            canonicalRequest.setContent(new ByteArrayInputStream(getDataJson(subscription).getBytes()));
        }

        AWSCredentialsProvider credentialsProvider =
            new CognitoCachingCredentialsProvider(applicationContext, identityPoolId, Regions.fromName(regionStr));
        String apiRegion = apiUrl.getAuthority().split("\\.")[2];
        if (connectionFlag){
            new AppSyncV4Signer(apiRegion, AppSyncV4Signer.ResourcePath.IAM_CONNECTION_RESOURCE_PATH)
                .sign(canonicalRequest, credentialsProvider.getCredentials());
        } else {
            new AppSyncV4Signer(apiRegion)
                .sign(canonicalRequest, credentialsProvider.getCredentials());
        }

        JSONObject authorizationMessage = new JSONObject();
        Map<String, String> signedHeaders = canonicalRequest.getHeaders();
        try {
            for(Map.Entry headerEntry : signedHeaders.entrySet()) {
                if (!headerEntry.getKey().equals("host")) {
                    authorizationMessage.put((String) headerEntry.getKey(), headerEntry.getValue());
                } else {
                    authorizationMessage.put("host", getHost(getApiUrl(awsConfiguration)));
                }
            }
        } catch (JSONException | MalformedURLException e) {
            throw new RuntimeException("Error constructing authorization message json", e);
        }
        return authorizationMessage;
    }

    private static JSONObject getAuthorizationDetailsForUserpools(
            AWSConfiguration awsConfiguration, Context applicationContext) {
        CognitoUserPool cognitoUserPool = new CognitoUserPool(applicationContext, awsConfiguration);
        BasicCognitoUserPoolsAuthProvider basicCognitoUserPoolsAuthProvider = new BasicCognitoUserPoolsAuthProvider(cognitoUserPool);
        try {
            return new JSONObject()
                .put("host", getHost(getApiUrl(awsConfiguration)))
                .put("Authorization", basicCognitoUserPoolsAuthProvider.getLatestAuthToken());
        } catch (JSONException | MalformedURLException exception) {
            throw new RuntimeException("Error constructing authorization message JSON.", exception);
        }
    }

    private static JSONObject getAuthorizationDetailsForOidc(
            AWSConfiguration awsConfiguration, OidcAuthProvider mOidcAuthProvider) {
        try {
            return new JSONObject()
                .put("host", getHost(getApiUrl(awsConfiguration)))
                .put("Authorization", mOidcAuthProvider.getLatestAuthToken());
        } catch (JSONException | MalformedURLException e) {
            throw new RuntimeException("Error constructing authorization message json", e);
        }
    }

    private static String getHost(String apiUrl) throws MalformedURLException {
        return new URL(apiUrl).getHost();
    }

    private static String getDataJson(Subscription subscription) {
        try {
            return new JSONObject()
                .put("query", subscription.queryDocument())
                .put("variables", new JSONObject(subscription.variables().valueMap()))
                .toString();
        } catch (JSONException jsonException) {
            throw new RuntimeException("Error constructing JSON object", jsonException);
        }
    }

    private static String getApiUrl(AWSConfiguration awsConfiguration) throws JSONException {
        return awsConfiguration.optJsonObject("AppSync").getString("ApiUrl");
    }

    /**
     * Utility to create a ISO 8601 compliant timestamps.
     */
    static final class ISO8601Timestamp {
        static String now() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
            return formatter.format(new Date());
        }
    }
}
