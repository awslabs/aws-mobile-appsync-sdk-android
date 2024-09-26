/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.appsync.retry.RetryInterceptor;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloException;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Manages the lifecycle of a single WebSocket connection,
 * and multiple GraphQL subscriptions that work on top of it.
 */
final class WebSocketConnectionManager {
    private static final String TAG = WebSocketConnectionManager.class.getName();
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private final Context applicationContext;
    private final String serverUrl;
    private final SubscriptionAuthorizer subscriptionAuthorizer;
    private final Map<String, SubscriptionResponseDispatcher<?, ?, ?>> subscriptions;
    private final ApolloResponseBuilder apolloResponseBuilder;
    private final TimeoutWatchdog watchdog;
    private final boolean subscriptionsAutoReconnect;
    private WebSocket websocket;

    WebSocketConnectionManager(
            Context context,
            String serverUrl,
            SubscriptionAuthorizer subscriptionAuthorizer,
            ApolloResponseBuilder apolloResponseBuilder,
            boolean subscriptionsAutoReconnect) {
        this.applicationContext = context.getApplicationContext();
        this.serverUrl = serverUrl;
        this.subscriptionAuthorizer = subscriptionAuthorizer;
        this.subscriptions = new ConcurrentHashMap<>();
        this.apolloResponseBuilder = apolloResponseBuilder;
        this.watchdog = new TimeoutWatchdog();
        this.subscriptionsAutoReconnect = subscriptionsAutoReconnect;
    }

    /**
     * Requests a new subscription.
     * Subscription events will be posted to the provided callback.
     * If there is no active WebSocket, one will be created.
     * If there is already an active WebSocket, it will be recycled, and will host this subscription, too.
     * @param subscription subcription object (e.g. "NewCommentSubscription," etc.
     * @param callback Callback to invoke when subscription messages are received over socket
     * @param <D> Type of data returned for subscription
     * @param <T> Type of data returned to callback (usually same as response data, D.)
     * @param <V> Type of variables object used to satisfy parameterization
     *            in the subscription object
     * @return A unique ID that refers to a successfully-established subscription
     */
    synchronized <D extends Operation.Data, T, V extends Operation.Variables> String requestSubscription(
            @NonNull Subscription<D, T, V> subscription,
            @NonNull AppSyncSubscriptionCall.Callback<T> callback) {
        if (websocket == null) {
            websocket = createWebSocket();
        }

        String subscriptionId = UUID.randomUUID().toString();
        startSubscription(subscription, callback, subscriptionId);

        final SubscriptionResponseDispatcher<D, T, V> subscriptionResponseDispatcher =
            new SubscriptionResponseDispatcher<>(subscription, callback, apolloResponseBuilder);
        subscriptions.put(subscriptionId, subscriptionResponseDispatcher);

        return subscriptionId;
    }

    private synchronized void startSubscription(
            @NonNull Subscription<?, ?, ?> subscription,
            @NonNull AppSyncSubscriptionCall.Callback<?> callback,
            String subscriptionId) {
        try {
            // Check result to avoid silent failure
            boolean enqueued = websocket.send(new JSONObject()
                .put("id", subscriptionId)
                .put("type", "start")
                .put("payload", new JSONObject()
                    .put("data", (new JSONObject()
                        .put("query", subscription.queryDocument())
                        .put("variables", new JSONObject(subscription.variables().valueMap()))).toString())
                    .put("extensions", new JSONObject()
                        .put("authorization", subscriptionAuthorizer.getAuthorizationDetails(false, subscription))))
                .toString()
            );
            if (!enqueued) {
                callback.onFailure(new ApolloException("WebSocket communication failed."));
            }
        } catch (JSONException jsonException) {
            throw new RuntimeException("Failed to construct subscription registration message.", jsonException);
        }
    }

    private WebSocket createWebSocket() {
        Request.Builder requestBuilder = new Request.Builder()
            .url(getConnectionRequestUrl())
            .addHeader("Sec-WebSocket-Protocol", "graphql-ws");

        try {
            JSONObject authorizationDetails = subscriptionAuthorizer.getConnectionAuthorizationDetails();
            for (Iterator<String> it = authorizationDetails.keys(); it.hasNext(); ) {
                String key = it.next();
                requestBuilder.addHeader(key, authorizationDetails.getString(key));
            }
        } catch (JSONException jsonException) {
            throw new RuntimeException("Failed to add authorization details to request", jsonException);
        }

        Request request = requestBuilder.build();

        websocket = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
            .newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(@NonNull final WebSocket webSocket, @NonNull final Response response) {
                    if (subscriptionsAutoReconnect) {
                        reportSuccessfulConnection();
                    }
                    sendConnectionInit(websocket);
                }

                @Override
                public void onMessage(@NonNull final WebSocket webSocket, @NonNull final String message) {
                    processMessage(websocket, message);
                }

                @Override
                public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    webSocket.close(NORMAL_CLOSURE_STATUS, null);
                    notifyAllSubscriptionsCompleted();
                }

                @Override
                public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable failure, Response response) {
                    if (subscriptionsAutoReconnect) {
                        scheduleReconnect();
                    }
                    notifyFailure(failure);
                }
            });

        return websocket;
    }

    private void sendConnectionInit(WebSocket webSocket) {
        try {
            webSocket.send(new JSONObject()
                .put("type", "connection_init")
                .toString());
        } catch (JSONException jsonException) {
            notifyFailure(jsonException);
        }
    }

    private void processMessage(WebSocket webSocket, String message) {
        try {
            processJsonMessage(webSocket, message);
        } catch (JSONException jsonException) {
            notifyFailure(jsonException);
        }
    }

    private void processJsonMessage(WebSocket webSocket, String message) throws JSONException {
        final JSONObject jsonMessage = new JSONObject(message);
        final String messageTypeAsString = jsonMessage.getString("type");
        final MessageType messageType = MessageType.fromString(messageTypeAsString);

        switch (messageType) {
            case CONNECTION_ACK:
                final String connectionTimeoutString =
                    jsonMessage.getJSONObject("payload").getString("connectionTimeoutMs");
                watchdog.start(webSocket, Integer.parseInt(connectionTimeoutString));
                break;
            case SUBSCRIPTION_ACK:
                notifySubscriptionStarted(jsonMessage.getString("id"));
                Log.d(TAG, "Subscription created with id = " + jsonMessage.getString("id"));
                break;
            case SUBSCRIPTION_COMPLETED:
                notifySubscriptionCompleted(jsonMessage.getString("id"));
                break;
            case KEEP_ALIVE:
                watchdog.reset();
                break;
            case ERROR:
            case DATA:
                notifySubscriptionData(jsonMessage.getString("id"), jsonMessage.getString("payload"));
                break;
            default:
                notifyFailure(new ApolloException("Got unknown message type: " + messageType));
        }
    }

    private static final int MSG_RECONNECT = 0;
    private HandlerThread reconnectThread = null;
    private Handler reconnectHandler = null;
    private ConnectivityWatcher connectivityWatcher = null;
    private final Object reconnectionLock = new Object();
    private int reconnectionCount = 0;

    private void scheduleReconnect() {
        synchronized (reconnectionLock) {
            if (reconnectHandler != null && reconnectHandler.hasMessages(MSG_RECONNECT)) {
                return;
            }

            if (reconnectionCount == 0) {
                reconnectThread = new HandlerThread("AWSAppSyncWebSocketReconnectionThread");
                reconnectThread.start();
                reconnectHandler = new Handler(reconnectThread.getLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(@NonNull Message msg) {
                        if (msg.what == MSG_RECONNECT) {
                            retryAllSubscriptions();
                            return true;
                        }
                        return false;
                    }
                });

                ConnectivityWatcher.Callback callback = new ConnectivityWatcher.Callback() {
                    @Override
                    public void onConnectivityChanged(boolean isNetworkConnected) {
                        if (isNetworkConnected) reportNetworkUp();
                    }
                };
                if (connectivityWatcher == null) {
                    connectivityWatcher = new ConnectivityWatcher(applicationContext, callback);
                } else {
                    connectivityWatcher.unregister();
                }
                connectivityWatcher.register();
            }

            reconnectionCount++;
            int delayMillis = RetryInterceptor.calculateBackoff(reconnectionCount);
            Log.v(TAG, "Scheduling reconnection after [" + delayMillis + "] ms.");
            reconnectHandler.sendEmptyMessageDelayed(MSG_RECONNECT, delayMillis);
        }
    }

    private void reportSuccessfulConnection() {
        synchronized (reconnectionLock) {
            if (reconnectionCount == 0) {
                return;
            }
            Log.v(TAG, "Successful connection reported!");

            connectivityWatcher.unregister();
            connectivityWatcher = null;
            reconnectThread.quit();
            reconnectThread = null;
            reconnectHandler = null;
            reconnectionCount = 0;
        }
    }

    private void reportNetworkUp() {
        synchronized (reconnectionLock) {
            if (reconnectionCount == 0) {
                return;
            }
            Log.v(TAG, "Network is up. Trying to reconnect immediately.");

            reconnectHandler.removeMessages(MSG_RECONNECT);
            reconnectHandler.sendEmptyMessage(MSG_RECONNECT);
        }
    }

    private synchronized void retryAllSubscriptions() {
        // WebSocket that has failed cannot be used any more so recreate one
        if (websocket != null) {
            websocket.cancel();
        }

        try {
            createWebSocket();
        } catch (AmazonClientException e) {
            Log.v(TAG, "Failed to create WebSocket: " + e);
            scheduleReconnect();
            return;
        }

        for (Map.Entry<String, SubscriptionResponseDispatcher<?, ?, ?>> entry : subscriptions.entrySet()) {
            SubscriptionResponseDispatcher<?, ?, ?> dispatcher = entry.getValue();
            startSubscription(dispatcher.getSubscription(), dispatcher.getCallback(), entry.getKey());
        }
    }

    private void notifyAllSubscriptionsCompleted() {
        // TODO: if the connection closes, but our subscription didn't ask for that,
        // is that a failure, from its standpoint? Or not?
        for (SubscriptionResponseDispatcher<?,?,?> dispatcher : new HashSet<>(subscriptions.values())) {
            dispatcher.getCallback().onCompleted();
        }
    }

    private void notifySubscriptionStarted(String subscriptionId) {
        final SubscriptionResponseDispatcher<?,?,?> dispatcher = subscriptions.get(subscriptionId);
        if (dispatcher != null) {

            AppSyncSubscriptionCall.Callback<?> callback = dispatcher.getCallback();

            if (callback instanceof AppSyncSubscriptionCall.StartedCallback<?>) {
                ((AppSyncSubscriptionCall.StartedCallback<?>)callback).onStarted();
            }
        }
    }

    private void notifySubscriptionCompleted(String subscriptionId) {
        final SubscriptionResponseDispatcher<?,?,?> dispatcher = subscriptions.get(subscriptionId);
        if (dispatcher != null) {
            dispatcher.getCallback().onCompleted();
        }
    }

    private void notifyFailure(Throwable failure) {
        for (SubscriptionResponseDispatcher<?,?,?> dispatcher : new HashSet<>(subscriptions.values())) {
            dispatcher.getCallback().onFailure(new ApolloException("Subscription failed.", failure));
        }
    }

    private void notifySubscriptionData(String subscriptionId, String data) {
        final SubscriptionResponseDispatcher<?,?,?> dispatcher = subscriptions.get(subscriptionId);
        if (dispatcher != null) {
            dispatcher.invokeResponseCallback(data);
        }
    }

    synchronized void releaseSubscription(String subscriptionId) {
        if (!subscriptions.containsKey(subscriptionId)) {
            throw new IllegalArgumentException("No existing subscription with the given id.");
        }

        try {
            websocket.send(new JSONObject()
                .put("type", "stop")
                .put("id", subscriptionId)
                .toString());
        } catch (JSONException jsonException) {
            throw new RuntimeException("Failed to construct subscription release message.", jsonException);
        }

        subscriptions.remove(subscriptionId);

        // If we have zero subscriptions, close the WebSocket
        if (subscriptions.size() == 0) {
            watchdog.stop();
            websocket.close(NORMAL_CLOSURE_STATUS, "No active subscriptions");
            websocket = null;
        }
    }

    /*
     * Discover WebSocket endpoint from the appsync endpoint.
     * AppSync endpoint : https://xxxxxxxxxxxx.appsync-api.ap-southeast-2.amazonaws.com/graphql
     * Discovered WebSocket endpoint : wss:// xxxxxxxxxxxx.appsync-realtime-api.ap-southeast-2.amazonaws.com/graphql
     */
    private String getConnectionRequestUrl() {

        URL appSyncEndpoint = null;
        try {
            appSyncEndpoint = new URL(serverUrl);
        } catch (MalformedURLException malformedUrlException) {
            // throwing in a second ...
        }
        if (appSyncEndpoint == null) {
            throw new RuntimeException("Malformed Api Url: " + serverUrl);
        }

        DomainType domainType = DomainType.from(serverUrl);

        String authority = appSyncEndpoint.getHost();
        if (domainType == DomainType.STANDARD) {
            authority = authority.replace("appsync-api", "appsync-realtime-api");
        }

        String path = appSyncEndpoint.getPath();
        if (domainType == DomainType.CUSTOM) {
            path = path + "/realtime";
        }

        return new Uri.Builder()
            .scheme("wss")
            .authority(authority)
            .appendPath(path)
            .build()
            .toString();
    }

    static final class SubscriptionResponseDispatcher<D extends Operation.Data, T, V extends Operation.Variables> {
        private final Subscription<D, T, V> subscription;
        private final AppSyncSubscriptionCall.Callback<T> callback;
        private final ApolloResponseBuilder apolloResponseBuilder;

        SubscriptionResponseDispatcher(
                Subscription<D, T, V> subscription,
                AppSyncSubscriptionCall.Callback<T> callback,
                ApolloResponseBuilder apolloResponseBuilder) {
            this.subscription = subscription;
            this.callback = callback;
            this.apolloResponseBuilder = apolloResponseBuilder;
        }

        Subscription<D, T, V> getSubscription() {
            return subscription;
        }

        AppSyncSubscriptionCall.Callback<T> getCallback() {
            return callback;
        }

        void invokeResponseCallback(String message) {
            callback.onResponse(apolloResponseBuilder.buildResponse(message, subscription));
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) return true;
            if (thatObject == null || getClass() != thatObject.getClass()) return false;

            SubscriptionResponseDispatcher<?, ?, ?> that = (SubscriptionResponseDispatcher<?, ?, ?>) thatObject;

            if (getSubscription() != null ? !getSubscription().equals(that.getSubscription()) : that.getSubscription() != null)
                return false;
            return getCallback() != null ? getCallback().equals(that.getCallback()) : that.getCallback() == null;
        }

        @Override
        public int hashCode() {
            int result = getSubscription() != null ? getSubscription().hashCode() : 0;
            result = 31 * result + (getCallback() != null ? getCallback().hashCode() : 0);
            return result;
        }
    }

    /**
     * The "type" field in a payload returned from the WebSocket server may have
     * one of the following values.
     */
    enum MessageType {
        /**
         * A Keep-Alive message is sent to the client to refresh the connection timeout.
         */
        KEEP_ALIVE("ka"),

        /**
         * Server sends this back in response to client's request to connection_init.
         */
        CONNECTION_ACK("connection_ack"),

        /**
         * Server sends back this message type when a subscription is completed (in response to a "stop").
         */
        SUBSCRIPTION_COMPLETED("complete"),

        /**
         * Server sends a "start_ack" in response to client's request to "start" a subscription.
         */
        SUBSCRIPTION_ACK("start_ack"),

        /**
         * Server sends back an error, such as if the server is already at its connection limit.
         */
        ERROR("error"),

        /**
         * Server sends back "data" for a subscription.
         */
        DATA("data");

        private final String messageType;

        MessageType(String messageType) {
            this.messageType = messageType;
        }

        @NonNull
        @Override
        public String toString() {
            return messageType;
        }

        static MessageType fromString(String messageType) {
            for (MessageType possibleMatch : MessageType.values()) {
                if (possibleMatch.toString().equals(messageType)) {
                    return possibleMatch;
                }
            }
            throw new IllegalArgumentException("Invalid message type string");
        }
    }
}
