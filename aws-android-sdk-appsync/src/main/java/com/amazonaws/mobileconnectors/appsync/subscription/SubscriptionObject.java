/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.subscription;

import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.apollographql.apollo.api.Operation;
import com.amazonaws.apollographql.apollo.api.Response;
import com.amazonaws.apollographql.apollo.api.Subscription;
import com.amazonaws.apollographql.apollo.exception.ApolloException;
import com.amazonaws.apollographql.apollo.exception.ApolloParseException;
import com.amazonaws.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.amazonaws.apollographql.apollo.internal.response.OperationResponseParser;
import com.amazonaws.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

public class SubscriptionObject<D extends Operation.Data, T, V extends Operation.Variables> {
    private static final String TAG = SubscriptionObject.class.getSimpleName();
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);

    public Subscription<D, T, V> subscription;
    public Set<String> topics;
    public Set<AppSyncSubscriptionCall.Callback> listeners;
    public ScalarTypeAdapters scalarTypeAdapters;
    public ResponseNormalizer<Map<String,Object>> normalizer;
    private boolean cancelled = false;

    public SubscriptionObject() {
        //Initialize topics and listeners.
        topics = new HashSet<>();
        listeners = new HashSet<>();
    }

    //Getter for listeners
    public Set<AppSyncSubscriptionCall.Callback> getListeners() {
        return listeners;
    }

    //Getter for topics
    public Set<String> getTopics() {
        return topics;
    }

    //Add listener
    public void addListener(AppSyncSubscriptionCall.Callback l) {
        Log.v(TAG, "Adding listener to " + this);
        listeners.add(l);
    }

    //Set cancelled status
    void setCancelled() {
        cancelled = true;
    }

    //get cancelled status
    boolean isCancelled() {
        return cancelled;
    }

    public void onMessage(final String msg) {
        try {
            //TODO: Check why is this being converted to a Response Body
            ResponseBody messageBody = ResponseBody.create(msg, MEDIA_TYPE);
            OperationResponseParser<D, T> parser = new OperationResponseParser<>(
                    subscription,
                    subscription.responseFieldMapper(),
                    scalarTypeAdapters,
                    normalizer);
            Response<T> parsedResponse = parser.parse(messageBody.source());

            if (parsedResponse.hasErrors()) {
                Log.w(TAG, "Errors detected in parsed subscription message");
            }
            //TODO: Check why the message is this is not done in an else clause
            propagateMessageToAllListeners(parsedResponse);
        } catch (Exception rethrown) {
            Log.e(TAG, "Failed to parse: " + msg, rethrown);
            notifyErrorToAllListeners(new ApolloParseException("Failed to parse http response", rethrown));
        }
    }

    public void onFailure(final ApolloException e) {
        if (e.getCause() instanceof SubscriptionDisconnectedException) {
            notifyDisconnectionEventToAllListeners();
        } else {
            notifyErrorToAllListeners(e);
        }
    }

    //Convenience method to propagate messages received on the subscription to all registered listeners
    private void propagateMessageToAllListeners(Response<T> data) {
        for (AppSyncSubscriptionCall.Callback listener : listeners) {
            Log.v(TAG, "Propagating message to : " + listener.toString());
            listener.onResponse(data);
        }
    }

    //Convenience method to notify all registered listeners that the subscription has been terminated.
    private void notifyDisconnectionEventToAllListeners() {
        for (AppSyncSubscriptionCall.Callback listener : listeners) {
            //Let all listeners know the connection was disconnected.
            listener.onCompleted();
        }
    }

    //Convenience method to notify all registered listeners that an error has occured on the subscription.
    private void notifyErrorToAllListeners(ApolloException e) {
        for (AppSyncSubscriptionCall.Callback listener : listeners) {
            listener.onFailure(e);
        }
    }
}
