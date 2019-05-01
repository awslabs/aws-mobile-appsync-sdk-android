/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * ConflictResolverInterface.
 */

public interface ConflictResolverInterface {

    public void resolveConflict(@Nonnull ConflictResolutionHandler handler,
                                @Nonnull JSONObject serverState,
                                @Nonnull JSONObject clientState,
                                @Nonnull String recordIdentifier,
                                @Nonnull String operationType);

}
