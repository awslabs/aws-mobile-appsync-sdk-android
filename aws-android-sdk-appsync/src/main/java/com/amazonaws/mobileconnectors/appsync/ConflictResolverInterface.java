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

package com.amazonaws.mobileconnectors.appsync;

import com.apollographql.apollo.api.Mutation;

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
