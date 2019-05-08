/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.sigv4;

public interface CognitoUserPoolsAuthProvider {
    public String getLatestAuthToken();
}
