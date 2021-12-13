/*
 * Copyright 2021 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type of domain specified in API endpoint.
 */
enum DomainType {
    /**
     * Standard domain type composed of AWS AppSync endpoint where unique-id is made of 26 alphanumeric characters.
     * See <a href="https://docs.aws.amazon.com/general/latest/gr/appsync.html">AppSync Endpoints</a>
     */
    STANDARD,

    /**
     * Custom domain defined by the user.
     */
    CUSTOM;

    private static final String STANDARD_ENDPOINT_REGEX =
            "^https://\\w{26}\\.appsync-api\\.\\w{2}(?:(?:-\\w{2,})+)-\\d\\.amazonaws.com/graphql$";

    /**
     * Get Domain type based on defined endpoint.
     * @param endpoint Endpoint defined in config.
     * @return {@link DomainType} based on supplied endpoint.
     */
    static DomainType from(String endpoint) {
        if (isRegexMatch(endpoint)) {
            return STANDARD;
        }

        return CUSTOM;
    }

    private static boolean isRegexMatch(String endpoint) {
        final Pattern pattern = Pattern.compile(DomainType.STANDARD_ENDPOINT_REGEX, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(endpoint);

        return matcher.matches();
    }
}
