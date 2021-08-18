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
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.AWSLambdaAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.AppSyncV4Signer;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicCognitoUserPoolsAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
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
    private final AWSConfiguration mAwsConfiguration;
    private final Context mApplicationContext;
    private final OidcAuthProvider mOidcAuthProvider;
    private final AWSCredentialsProvider mCredentialsProvider;
    private final CognitoUserPoolsAuthProvider mCognitoUserPoolsAuthProvider;
    private final AWSLambdaAuthProvider mAWSLambdaAuthProvider;
    private final String mServerUrl;
    private final APIKeyAuthProvider mApiKeyProvider;

    SubscriptionAuthorizer(AWSAppSyncClient.Builder builder) {
        this.mAwsConfiguration = builder.mAwsConfiguration;
        this.mApplicationContext = builder.mContext;
        this.mOidcAuthProvider = builder.mOidcAuthProvider;
        this.mCredentialsProvider = builder.mCredentialsProvider;
        this.mCognitoUserPoolsAuthProvider = builder.mCognitoUserPoolsAuthProvider;
        this.mAWSLambdaAuthProvider = builder.mAWSLambdaAuthProvider;
        this.mServerUrl = builder.mServerUrl;
        this.mApiKeyProvider = builder.mApiKey;
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
            authMode = mAwsConfiguration.optJsonObject("AppSync").getString("AuthMode");
        } catch (JSONException e) {
            throw new RuntimeException("Failed to read AuthMode from awsconfiguration.json", e);
        }

        // Construct the Json based on the Auth Mode
        switch (authMode) {
            case "API_KEY" :
                return getAuthorizationDetailsForApiKey();
            case "AWS_IAM" :
                return getAuthorizationDetailsForIAM(connectionFlag, subscription);
            case "AMAZON_COGNITO_USER_POOLS" :
                return getAuthorizationDetailsForUserpools();
            case "OPENID_CONNECT" :
                return getAuthorizationDetailsForOidc();
            case "AWS_LAMBDA" :
                return getAuthorizationDetailsForAwsLambda();
            default :
                throw new RuntimeException("Invalid AuthMode read from awsconfiguration.json.");
        }
    }

    private JSONObject getAuthorizationDetailsForApiKey() {
        try {
            return new JSONObject()
                .put("host", getHost(mServerUrl))
                .put("x-amz-date", ISO8601Timestamp.now())
                .put("x-api-key", getApiKey());
        } catch (JSONException | MalformedURLException e) {
            throw new RuntimeException("Error constructing the authorization json for Api key. ", e);
        }
    }

    private JSONObject getAuthorizationDetailsForIAM(boolean connectionFlag,
                                                     Subscription subscription) throws JSONException {

        DefaultRequest canonicalRequest = new DefaultRequest("appsync");

        URI apiUrl;
        try {
            final String baseUrl = mServerUrl;
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

        String apiRegion = apiUrl.getAuthority().split("\\.")[2];
        if (connectionFlag){
            new AppSyncV4Signer(apiRegion, AppSyncV4Signer.ResourcePath.IAM_CONNECTION_RESOURCE_PATH)
                .sign(canonicalRequest, getCredentialsProvider().getCredentials());
        } else {
            new AppSyncV4Signer(apiRegion)
                .sign(canonicalRequest, getCredentialsProvider().getCredentials());
        }

        JSONObject authorizationMessage = new JSONObject();
        Map<String, String> signedHeaders = canonicalRequest.getHeaders();
        try {
            for(Map.Entry headerEntry : signedHeaders.entrySet()) {
                if (!headerEntry.getKey().equals("host")) {
                    authorizationMessage.put((String) headerEntry.getKey(), headerEntry.getValue());
                } else {
                    authorizationMessage.put("host", getHost(mServerUrl));
                }
            }
        } catch (JSONException | MalformedURLException e) {
            throw new RuntimeException("Error constructing authorization message json", e);
        }
        return authorizationMessage;
    }

    private JSONObject getAuthorizationDetailsForUserpools() {
        CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider = getCognitoUserPoolsAuthProvider();
        try {
            return new JSONObject()
                .put("host", getHost(mServerUrl))
                .put("Authorization", cognitoUserPoolsAuthProvider.getLatestAuthToken());
        } catch (JSONException | MalformedURLException exception) {
            throw new RuntimeException("Error constructing authorization message JSON.", exception);
        }
    }

    private CognitoUserPoolsAuthProvider getCognitoUserPoolsAuthProvider() {
        if (mCognitoUserPoolsAuthProvider != null) { return mCognitoUserPoolsAuthProvider; }

        CognitoUserPool cognitoUserPool = new CognitoUserPool(mApplicationContext, mAwsConfiguration);
        return new BasicCognitoUserPoolsAuthProvider(cognitoUserPool);
    }

    private AWSCredentialsProvider getCredentialsProvider() throws RuntimeException{
        if (mCredentialsProvider != null) { return mCredentialsProvider; }

        try {
            String regionStr = getRegion();
            String identityPoolId = getIdentityPoolId();

            return new CognitoCachingCredentialsProvider(
                    mApplicationContext,
                    identityPoolId,
                    Regions.fromName(regionStr)
            );
        } catch (JSONException e) {
            throw new RuntimeException("Error reading identity pool information from AWSConfiguration", e);
        }
    }

    private JSONObject getAuthorizationDetailsForOidc() {
        try {
            return new JSONObject()
                .put("host", getHost(mServerUrl))
                .put("Authorization", mOidcAuthProvider.getLatestAuthToken());
        } catch (JSONException | MalformedURLException e) {
            throw new RuntimeException("Error constructing authorization message json", e);
        }
    }

    private JSONObject getAuthorizationDetailsForAwsLambda() {
        try {
            return new JSONObject()
                    .put("host", getHost(mServerUrl))
                    .put("Authorization", mAWSLambdaAuthProvider.getLatestAuthToken());
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

    private String getRegion() throws JSONException {
         return mAwsConfiguration
                 .optJsonObject("CredentialsProvider")
                 .getJSONObject("CognitoIdentity")
                 .getJSONObject(mAwsConfiguration.getConfiguration())
                 .getString("Region");
    }

    private String getIdentityPoolId() throws JSONException {
         return mAwsConfiguration
                 .optJsonObject("CredentialsProvider")
                 .getJSONObject("CognitoIdentity")
                 .getJSONObject(mAwsConfiguration.getConfiguration())
                 .getString("PoolId");
    }

    private String getApiKey() throws JSONException {
         return mApiKeyProvider != null
                 ? mApiKeyProvider.getAPIKey()
                 : mAwsConfiguration.optJsonObject("AppSync").getString("ApiKey");
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
