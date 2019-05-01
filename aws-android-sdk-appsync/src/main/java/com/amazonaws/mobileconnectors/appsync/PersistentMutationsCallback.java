/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

/**
 * PersistentMutationsCallback.
 */

public interface PersistentMutationsCallback {

    void onResponse(PersistentMutationsResponse response);

    void onFailure(PersistentMutationsError error);

}
