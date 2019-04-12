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

package com.amazonaws.mobileconnectors.appsync.sigv4;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobileconnectors.appsync.utils.StringUtils;
import com.amazonaws.util.VersionInfoUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

import android.util.Log;

public class AppSyncSigV4SignerInterceptor implements Interceptor {

    private static final String TAG = AppSyncSigV4SignerInterceptor.class.getSimpleName();

    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);
    private static final String SERVICE_NAME = "appsync";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String X_API_KEY = "x-api-key";
    private static final String AUTHORIZATION = "authorization";
    private static final String X_AMZ_SUBSCRIBER_ID = "x-amz-subscriber-id";

    private final AWSCredentialsProvider credentialsProvider;
    private final APIKeyAuthProvider apiKey;
    private final String subscriberUUID;

    private final CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider;
    private final OidcAuthProvider oidcAuthProvider;
    private final String awsRegion;
    private final AuthMode authMode;

    private enum AuthMode {
        API_KEY,
        IAM,
        OIDC_AUTHORIZATION_TOKEN,
        USERPOOLS_AUTHORIZATION_TOKEN
    }

    public AppSyncSigV4SignerInterceptor(APIKeyAuthProvider apiKey, final String awsRegion, final String subscriberUUID){
        this.apiKey = apiKey;
        this.awsRegion = awsRegion;
        this.credentialsProvider = null;
        this.cognitoUserPoolsAuthProvider = null;
        this.oidcAuthProvider = null;
        authMode = AuthMode.API_KEY;
        this.subscriberUUID = subscriberUUID;
    }

    public AppSyncSigV4SignerInterceptor(AWSCredentialsProvider credentialsProvider, final String awsRegion){
        this.credentialsProvider = credentialsProvider;
        this.awsRegion = awsRegion;
        this.apiKey = null;
        this.cognitoUserPoolsAuthProvider = null;
        this.oidcAuthProvider = null;
        authMode = AuthMode.IAM;
        subscriberUUID = null;
    }

    public AppSyncSigV4SignerInterceptor(CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider, final String awsRegion){
        this.credentialsProvider = null;
        this.awsRegion = awsRegion;
        this.apiKey = null;
        this.cognitoUserPoolsAuthProvider = cognitoUserPoolsAuthProvider;
        this.oidcAuthProvider = null;
        authMode = AuthMode.USERPOOLS_AUTHORIZATION_TOKEN;
        subscriberUUID = null;
    }

    public AppSyncSigV4SignerInterceptor(OidcAuthProvider oidcAuthProvider){
        this.credentialsProvider = null;
        this.awsRegion = null;
        this.apiKey = null;
        this.cognitoUserPoolsAuthProvider = null;
        this.oidcAuthProvider = oidcAuthProvider;
        authMode = AuthMode.OIDC_AUTHORIZATION_TOKEN;
        subscriberUUID = null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Log.d(TAG, "Signer Interceptor called");

        Request req = chain.request();

        //Clone the request into a new DefaultRequest object and populate it with credentials
        DefaultRequest dr = new DefaultRequest(SERVICE_NAME);
        //set the endpoint
        dr.setEndpoint(req.url().uri());
        //copy all the headers
        for(String headerName : req.headers().names()) {
            dr.addHeader(headerName, req.header(headerName));
        }
        //set the http method
        dr.setHttpMethod(HttpMethodName.valueOf(req.method()));

        //Add User Agent
        String userAgent = StringUtils.toHumanReadableAscii(VersionInfoUtils.getUserAgent());
        dr.addHeader(HEADER_USER_AGENT, userAgent);

        //write the body to a byte array stream.
        final Buffer buffer = new Buffer();
        req.body().writeTo(buffer);
        dr.setContent(buffer.inputStream());

        Buffer body = buffer.clone();

        //Sign or Decorate request with the required headers
        if (AuthMode.IAM.equals(authMode)) {
            //get the aws credentials from provider.
            try {
                //Get credentials - This will refresh the credentials if necessary
                AWSCredentials credentials = this.credentialsProvider.getCredentials();
                //sign the request
                new AppSyncV4Signer(this.awsRegion).sign(dr, credentials);
            } catch (Exception e) {
                throw new IOException("Failed to read credentials to sign the request.", e);
            }
        } else if (AuthMode.API_KEY.equals(authMode)) {
            dr.addHeader(X_API_KEY, apiKey.getAPIKey());
            if (subscriberUUID != null ) {
                Log.d(TAG,"Subscriber ID is " + subscriberUUID);
                dr.addHeader(X_AMZ_SUBSCRIBER_ID, subscriberUUID);
            }

        } else if (AuthMode.USERPOOLS_AUTHORIZATION_TOKEN.equals(authMode)) {
            try {
                dr.addHeader(AUTHORIZATION, cognitoUserPoolsAuthProvider.getLatestAuthToken());
            } catch (Exception e) {
                IOException ioe = new IOException("Failed to retrieve Cognito User Pools token.", e);
                throw ioe;
            }
        } else if (AuthMode.OIDC_AUTHORIZATION_TOKEN.equals(authMode)) {
            try {
                dr.addHeader(AUTHORIZATION, oidcAuthProvider.getLatestAuthToken());
            } catch (Exception e) {
                IOException ioe = new IOException("Failed to retrieve OIDC token.", e);
                throw ioe;
            }
        }

        //Copy the signed/credentialed request back into an OKHTTP Request object.
        Request.Builder okReqBuilder = new Request.Builder();

        //set the headers from default request, since it contains the signed headers as well.
        for (Map.Entry<String, String> e : (Set<Map.Entry<String, String>>)dr.getHeaders().entrySet()) {
            okReqBuilder.addHeader(e.getKey(), e.getValue());
        }

        //Set the URL and Method
        okReqBuilder.url(req.url());
        okReqBuilder.method(req.method(), RequestBody.create(JSON_MEDIA_TYPE, body.readByteArray()));

        //continue with chain.
        Response res = chain.proceed(okReqBuilder.build());
        return res;
    }

}
