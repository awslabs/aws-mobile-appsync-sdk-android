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

import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;

import java.util.concurrent.Semaphore;

/**
 * Basic retrieval of the Cognito Userpools token. The user must have already signed-in before using
 * this class as the retrieval mechanism in the {@link AWSAppSyncClient#builder()}
 */
public class BasicCognitoUserPoolsAuthProvider implements CognitoUserPoolsAuthProvider {

    private CognitoUserPool userPool;
    private String token;
    private String lastTokenRetrievalFailureMessage;

    public BasicCognitoUserPoolsAuthProvider(CognitoUserPool userPool) {
        this.userPool = userPool;
    }

    /**
     * Fetches token from the Cognito User Pools client for the current user.
     */
    private synchronized void fetchToken() {
        final Semaphore semaphore = new Semaphore(0);
        lastTokenRetrievalFailureMessage = null;
        userPool.getCurrentUser().getSessionInBackground(new AuthenticationHandler() {
            @Override
            public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                token = userSession.getAccessToken().getJWTToken();
                semaphore.release();
            }

            @Override
            public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                lastTokenRetrievalFailureMessage = "Cognito Userpools is not signed-in";
                semaphore.release();
            }

            @Override
            public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
                lastTokenRetrievalFailureMessage = "Cognito Userpools is not signed-in";
                semaphore.release();
            }

            @Override
            public void authenticationChallenge(ChallengeContinuation continuation) {
                lastTokenRetrievalFailureMessage = "Cognito Userpools is not signed-in";
                semaphore.release();
            }

            @Override
            public void onFailure(Exception exception) {
                lastTokenRetrievalFailureMessage = "Cognito Userpools failed to get session";
                semaphore.release();
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for Cognito Userpools token.", e);
        }

        if (lastTokenRetrievalFailureMessage != null) {
            throw new RuntimeException(lastTokenRetrievalFailureMessage);
        }
    }

    @Override
    public String getLatestAuthToken() {
        fetchToken();
        return token;
    }
}
