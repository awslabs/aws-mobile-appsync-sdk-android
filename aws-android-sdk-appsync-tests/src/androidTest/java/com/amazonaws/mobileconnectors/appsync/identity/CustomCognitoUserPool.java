/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.identity;

import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.util.Await;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;

import static android.support.test.InstrumentationRegistry.getTargetContext;

public final class CustomCognitoUserPool {
    private static final String TAG = CustomCognitoUserPool.class.getSimpleName();

    private CustomCognitoUserPool() {}

    @NonNull
    public static String setup() {
        // Sign in the user.
        Await.result((Await.ResultErrorEmitter<SignInResult, RuntimeException>) (onResult, onError) -> {
            DelegatingMobileClientCallback<SignInResult> callback =
                DelegatingMobileClientCallback.to(onResult, exception -> onError.accept(new RuntimeException(exception)));
            TestAWSMobileClient.instance(getTargetContext())
                .signIn("appsync-multi-auth-test-user", "P@ssw0rd!", null, callback);
        });

        // Build a custom cognito user pool.
        AWSConfiguration awsConfiguration = new AWSConfiguration(getTargetContext());
        awsConfiguration.setConfiguration("Custom");
        CognitoUserPool cognitoUserPool = new CognitoUserPool(getTargetContext(), awsConfiguration);

        // Get the ID token for this user.
        return Await.result((onResult, onError) ->
            cognitoUserPool.getUser("appsync-multi-auth-test-user").getSession(new AuthenticationHandler() {
                @Override
                public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                    onResult.accept(userSession.getIdToken().getJWTToken());
                }

                @Override
                public void getAuthenticationDetails(
                        AuthenticationContinuation authenticationContinuation, String userId) {
                    Log.d(TAG, "Sending password.");
                    authenticationContinuation.setAuthenticationDetails(new AuthenticationDetails(
                        "appsync-multi-auth-test-user", "P@ssw0rd!", null
                    ));
                    authenticationContinuation.continueTask();
                }

                @Override
                public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
                }

                @Override
                public void authenticationChallenge(ChallengeContinuation continuation) {
                }

                @Override
                public void onFailure(Exception exception) {
                    onError.accept(new RuntimeException(exception));
                }
            }));
    }
}
