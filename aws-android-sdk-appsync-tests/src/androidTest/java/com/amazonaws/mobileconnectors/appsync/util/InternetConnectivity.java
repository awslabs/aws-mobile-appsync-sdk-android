/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.mobileconnectors.appsync.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public final class InternetConnectivity {
    private InternetConnectivity() {}

    public static void goOnline() {
        RetryStrategies.linear(AirplaneMode::disable, InternetConnectivity::isOnline, 3, 2);
    }

    public static void goOffline() {
        RetryStrategies.linear(AirplaneMode::enable, InternetConnectivity::isOffline, 3, 2);
    }

    private static boolean isOnline() {
        int connectionTimeoutMs = (int) TimeUnit.SECONDS.toMillis(2);
        String host = "amazon.com";
        int port = 443;

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), connectionTimeoutMs);
            socket.close();
            return true;
        } catch (IOException socketFailure) {
            return false;
        }
    }

    private static boolean isOffline() {
        return !isOnline();
    }
}
