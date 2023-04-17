/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.apollographql.apollo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class TLS12SocketFactory extends SSLSocketFactory {

    private static final Object contextLock = new Object();
    public static final String TLSv1_2 = "TLSv1.2";
    private static final String[] SUPPORTED_PROTOCOLS = new String[] { TLSv1_2 };
    private static SSLContext sslContext = null;
    private final SSLSocketFactory delegate;

    @Nullable
    public static TLS12SocketFactory createTLS12SocketFactory() {
        return createTLS12SocketFactory(null);
    }

    @Nullable
    public static TLS12SocketFactory createTLS12SocketFactory(
            @Nullable SSLContext sslContext
    ) {
        try {
            return new TLS12SocketFactory(sslContext);
        } catch (Exception e) {
            return null;
        }
    }

    public static void fixTLSPre22(@Nonnull HttpsURLConnection connection) {
        fixTLSPre22(connection, createTLS12SocketFactory());
    }

    public static void fixTLSPre22(
            @Nonnull HttpsURLConnection connection,
            @Nullable TLS12SocketFactory tls12SocketFactory
    ) {
        if (tls12SocketFactory != null) {
            try {
                connection.setSSLSocketFactory(tls12SocketFactory);
            } catch (Exception e) {
                // Failed to enabled TLS1.2 on <= Android 22 device
            }
        }
    }

    private TLS12SocketFactory(@Nullable SSLContext customSSLContext)
            throws KeyManagementException, NoSuchAlgorithmException {

        if (customSSLContext != null) {
            delegate = customSSLContext.getSocketFactory();
        } else {
            // Cache SSLContext due to weight and hold static
            synchronized (contextLock) {
                if (sslContext == null) {
                    sslContext = SSLContext.getInstance(TLSv1_2);
                    sslContext.init(null, null, null);
                }
            }
            delegate = sslContext.getSocketFactory();
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return updateTLSProtocols(delegate.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return updateTLSProtocols(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return updateTLSProtocols(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return updateTLSProtocols(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return updateTLSProtocols(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return updateTLSProtocols(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket updateTLSProtocols(Socket socket) {
        if(socket instanceof SSLSocket) {
            try {
                boolean hasOldTLS = false;
                boolean hasTLS12 = false;
                String[] enabledProtocols = ((SSLSocket) socket).getEnabledProtocols();
                for (String protocol : enabledProtocols) {
                    if (protocol.equals("TLSv1.1") ||
                            protocol.equals("TLSv1") ||
                            protocol.equals("SSLv3")) {
                        hasOldTLS = true;
                    }
                    if (protocol.equals("TLSv1.2")) {
                        hasTLS12 = true;
                    }
                }
                if (hasOldTLS || !hasTLS12) {
                    ((SSLSocket) socket).setEnabledProtocols(SUPPORTED_PROTOCOLS);
                }
            } catch (Exception e) {
                // TLS 1.2 may not be supported on device
            }
        }
        return socket;
    }
}
