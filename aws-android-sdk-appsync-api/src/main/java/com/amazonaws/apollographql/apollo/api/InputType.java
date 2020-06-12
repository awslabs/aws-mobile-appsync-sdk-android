/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.apollographql.apollo.api;

import javax.annotation.Nonnull;

public interface InputType {
    @Nonnull InputFieldMarshaller marshaller();
}
