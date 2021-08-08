/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.identity;

import android.content.Context;

import androidx.test.InstrumentationRegistry;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.appsync.util.Sleep;
import com.amazonaws.regions.Regions;

// TODO: Why does this exist?
public final class DelayedCognitoCredentialsProvider implements AWSCredentialsProvider {
    private final AWSCredentialsProvider credentialsProvider;
    private final long credentialsDelay;

    public DelayedCognitoCredentialsProvider(String cognitoIdentityPoolID, Regions region, long credentialsDelay) {
        Context context = InstrumentationRegistry.getContext();
        this.credentialsProvider = new CognitoCachingCredentialsProvider(context, cognitoIdentityPoolID, region);
        this.credentialsDelay = credentialsDelay;
    }

    @Override
    public AWSCredentials getCredentials() {
        if (credentialsDelay > 0) {
            Sleep.milliseconds(credentialsDelay);
        }
        return credentialsProvider.getCredentials();
    }

    @Override
    public void refresh() {
        credentialsProvider.refresh();
    }
}
