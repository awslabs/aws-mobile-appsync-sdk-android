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

package com.amazonaws.mobileconnectors.appsync.subscription;

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.response.OperationResponseParser;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

public class SubscriptionObject<D extends Operation.Data, T, V extends Operation.Variables> {
    private final static String TAG = SubscriptionObject.class.getSimpleName();

    public Subscription<D, T, V> subscription;
    public Set<String> topics;
    public Set<AppSyncSubscriptionCall.Callback> listeners;
    public ScalarTypeAdapters scalarTypeAdapters;
    public ResponseNormalizer<Map<String,Object>> normalizer;

    public SubscriptionObject() {
        topics = new HashSet<>();
        listeners = new HashSet<>();
    }

    public Set<AppSyncSubscriptionCall.Callback> getListeners() {
        return listeners;
    }

    public Set<String> getTopics() {
        return topics;
    }

    public void addListener(AppSyncSubscriptionCall.Callback l) {
        Log.d(TAG, "Adding listener to " + this);
        listeners.add(l);
    }

    public void onMessage(final String msg) {
        try {
            ResponseBody messageBody = ResponseBody.create(MediaType.parse("text/plain"), msg);
            OperationResponseParser<D, T> parser = new OperationResponseParser(subscription,
                    subscription.responseFieldMapper(), scalarTypeAdapters, normalizer);
            Response<T> parsedResponse = parser.parse(messageBody.source());
            if (parsedResponse.hasErrors()) {
                Log.d(TAG, "Errors detected in parsed subscription message");
            }
            notifyAllMessage(parsedResponse);
        } catch (Exception rethrown) {
            Log.e(TAG, "Failed to parse: " + msg, rethrown);
            notifyAllError(new ApolloParseException("Failed to parse http response", rethrown));
        }
    }

    public void onFailure(final ApolloException e) {
        if (e.getCause() instanceof SubscriptionDisconnectedException) {
            notifyAllDisconnected();
        } else {
            notifyAllError(e);
        }
    }

    private void notifyAllMessage(Response<T> data) {
        for (AppSyncSubscriptionCall.Callback listener : listeners) {
            Log.d(TAG, "Messaging: " + listener.toString());
            listener.onResponse(data);
        }
    }

    private void notifyAllDisconnected() {
        for (AppSyncSubscriptionCall.Callback listener : listeners) {
            listener.onCompleted();
        }
    }

    private void notifyAllError(ApolloException e) {
        for (AppSyncSubscriptionCall.Callback listener : listeners) {
            listener.onFailure(e);
        }
    }
}
