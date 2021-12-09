/*
 * Copyright 2021 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import org.junit.Assert;
import org.junit.Test;

public class DomainTypeTest {
    private static final String STANDARD_URL =
            "https://abcdefghijklmnopqrstuvwxyz.appsync-api.us-west-2.amazonaws.com/graphql";
    private static final String CUSTOM_URL = "https://something.in.somedomain.com/graphql";

    /**
     * Test that Domain type is {@link DomainType#STANDARD} for generated URL.
     */
    @Test
    public void testStandardURLMatch() {
        Assert.assertEquals(DomainType.STANDARD, DomainType.from(STANDARD_URL));
    }

    /**
     * Test that Domain type is set to {@link DomainType#CUSTOM} for custom URLs.
     */
    @Test
    public void testCustomURLMatch() {
        Assert.assertEquals(DomainType.CUSTOM, DomainType.from(CUSTOM_URL));
    }
}