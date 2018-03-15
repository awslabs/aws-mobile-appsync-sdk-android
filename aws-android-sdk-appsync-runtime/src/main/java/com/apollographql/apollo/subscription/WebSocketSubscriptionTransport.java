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

package com.apollographql.apollo.subscription;

import com.apollographql.apollo.api.Subscription;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class WebSocketSubscriptionTransport implements SubscriptionTransport {
    private final Request webSocketRequest;
    private final WebSocket.Factory webSocketConnectionFactory;
    private final OkHttpClient mOkHttpClient;
    private final Callback callback;
    final AtomicReference<WebSocket> webSocket = new AtomicReference<>();
    final AtomicReference<WebSocketListener> webSocketListener = new AtomicReference<>();

    WebSocketSubscriptionTransport(Request webSocketRequest, WebSocket.Factory webSocketConnectionFactory,
                                   Callback callback) {
        this.webSocketRequest = webSocketRequest;
        this.webSocketConnectionFactory = webSocketConnectionFactory;
        this.callback = callback;
        this.mOkHttpClient = null;
    }

    WebSocketSubscriptionTransport(OkHttpClient httpClient, Callback callback) {
        this.webSocketRequest = null;
        this.webSocketConnectionFactory = null;
        this.callback = callback;
        this.mOkHttpClient = httpClient;
    }

    @Override
    public void connect(Subscription subscription) {
//        WebSocketListener webSocketListener = new WebSocketListener(this);
//        if (!this.webSocketListener.compareAndSet(null, webSocketListener)) {
//            throw new IllegalStateException("Already connected");
//        }
//        webSocket.set(webSocketConnectionFactory.newWebSocket(webSocketRequest, webSocketListener));
    }

    @Override
    public void disconnect(OperationClientMessage message) {
        WebSocket socket = webSocket.getAndSet(null);

        if (socket != null) {
            socket.close(1001, message.toJsonString());
        }

        release();
    }

    @Override
    public void send(OperationClientMessage message) {
        WebSocket socket = webSocket.get();
        if (socket == null) {
            throw new IllegalStateException("Not connected");
        }
        socket.send(message.toJsonString());
    }

    void onOpen() {
        callback.onConnected();
    }

    void onMessage(OperationServerMessage message) {
        callback.onMessage(message);
    }

    void onFailure(Throwable t) {
        try {
            callback.onFailure(t);
        } finally {
            release();
        }
    }

    void release() {
        WebSocketListener socketListener = webSocketListener.getAndSet(null);
        if (socketListener != null) {
            socketListener.release();
        }
        webSocket.set(null);
    }

    static final class WebSocketListener extends okhttp3.WebSocketListener {
        final WeakReference<WebSocketSubscriptionTransport> delegateRef;

        WebSocketListener(WebSocketSubscriptionTransport delegate) {
            delegateRef = new WeakReference<>(delegate);
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            WebSocketSubscriptionTransport delegate = delegateRef.get();
            if (delegate != null) {
                delegate.onOpen();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            WebSocketSubscriptionTransport delegate = delegateRef.get();
            if (delegate != null) {
                OperationServerMessage message = OperationServerMessage.fromJsonString(text);
                delegate.onMessage(message);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            WebSocketSubscriptionTransport delegate = delegateRef.get();
            if (delegate != null) {
                delegate.onFailure(t);
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            WebSocketSubscriptionTransport delegate = delegateRef.get();
            if (delegate != null) {
                delegate.release();
            }
        }

        @Override public void onClosed(WebSocket webSocket, int code, String reason) {
            WebSocketSubscriptionTransport delegate = delegateRef.get();
            if (delegate != null) {
                delegate.release();
            }
        }

        void release() {
            delegateRef.clear();
        }
    }

    public static final class Factory implements SubscriptionTransport.Factory {
//        private final Request webSocketRequest;
//        private final WebSocket.Factory webSocketConnectionFactory = null;
        private final OkHttpClient httpClient;

        public Factory(OkHttpClient httpClient/*@Nonnull String webSocketUrl, @Nonnull WebSocket.Factory webSocketConnectionFactory*/) {
//            this.webSocketRequest = new Request.Builder()
//                    .url(checkNotNull(webSocketUrl, "webSocketUrl == null"))
//                    .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
//                    .addHeader("Cookie", "")
//                    .build();
//            this.webSocketConnectionFactory = checkNotNull(webSocketConnectionFactory, "webSocketConnectionFactory == null");
            this.httpClient = httpClient;
        }

        @Override
        public SubscriptionTransport create(@Nonnull Callback callback) {
            checkNotNull(callback, "callback == null");
            //return new WebSocketSubscriptionTransport(webSocketRequest, webSocketConnectionFactory, callback);
            return new WebSocketSubscriptionTransport(httpClient, callback);
        }
    }
}
