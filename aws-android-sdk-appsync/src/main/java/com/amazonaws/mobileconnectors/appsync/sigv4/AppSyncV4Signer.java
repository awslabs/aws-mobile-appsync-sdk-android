/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.sigv4;

import com.amazonaws.Request;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.util.BinaryUtils;

import java.io.InputStream;
import java.net.URI;

public class AppSyncV4Signer extends AWS4Signer {

    private static final String TAG = AppSyncV4Signer.class.getSimpleName();

    private static final String SERVICE_NAME_SCOPE = "appsync";

    private static final String RESOURCE_PATH = "/graphql";

    private ResourcePath resourcePath;

    public AppSyncV4Signer(String region) {
        this(region, ResourcePath.DEFAULT_RESOURCE_PATH);
    }

    public AppSyncV4Signer(String region, ResourcePath resourcePath) {
        super(true);
        this.resourcePath = resourcePath;
        setRegionName(region);
    }

    @Override
    protected String extractServiceName(URI endpoint) {
        return SERVICE_NAME_SCOPE;
    }

    @Override
    protected String getCanonicalizedResourcePath(String resourcePath) {
        return (this.resourcePath != null && this.resourcePath.equals(ResourcePath.IAM_CONNECTION_RESOURCE_PATH)) ?
                RESOURCE_PATH + "/connect" : RESOURCE_PATH;
    }

    @Override
    protected String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        return (this.resourcePath != null && this.resourcePath.equals(ResourcePath.IAM_CONNECTION_RESOURCE_PATH)) ?
                RESOURCE_PATH + "/connect" : RESOURCE_PATH;
    }

    @Override
    protected String calculateContentHash(Request<?> request) {
        final InputStream payloadStream = request.getContent();
        payloadStream.mark(-1);
        final String contentSha256 = BinaryUtils.toHex(hash(payloadStream));
        // We will not reset this as ok http does not allow reset of stream.
        return contentSha256;
    }

    /**
     * URL in the canonical request for connecting to subscription WebSocket
     * via AWS_IAM requires "/connect" appended to it. This is flag to let us
     * know when we're in that situation.
     */
    public enum ResourcePath {
        IAM_CONNECTION_RESOURCE_PATH,
        DEFAULT_RESOURCE_PATH;
    }
}

