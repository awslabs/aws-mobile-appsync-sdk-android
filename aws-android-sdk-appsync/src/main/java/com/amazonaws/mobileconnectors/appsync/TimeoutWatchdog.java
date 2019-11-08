/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import okhttp3.WebSocket;

/**
 * Closes the WebSocket connection if the time remaining has elapsed.
 * Enables resetting of the watchdog remaining time.
 */
final class TimeoutWatchdog {
    private static final String TAG = TimeoutWatchdog.class.getSimpleName();
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private final Handler handler;

    private Runnable cleanupRunnable;
    private long connectionTimeoutMs;

    TimeoutWatchdog() {
        // TODO: this should be a HandlerThread, don't use MainLooper.
        this.handler = new Handler(Looper.getMainLooper());
        this.connectionTimeoutMs = -1;
        this.cleanupRunnable = null;
    }

    /**
     * Starts a new timer. If a timer is already in progress, it will be stopped,
     * without running to completion. Instead, the new timer will be used in its place.
     * @param webSocket A WebSocket to close after connection timeout has elapsed
     * @param connectionTimeoutMs After this period of time, the webSocket is closed
     */
    synchronized void start(final WebSocket webSocket, long connectionTimeoutMs) {
        if (webSocket == null) {
            throw new NullPointerException("Passed null webSocket to watchdog.");
        } else if (connectionTimeoutMs <= 0) {
            throw new IllegalArgumentException("connectionTimeoutMs must be > 0.");
        }

        // If there's an existing timer, stop it.
        stop();

        // Now, make a new timer, and save the connection timeout.
        this.connectionTimeoutMs = connectionTimeoutMs;
        cleanupRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "WebSocket closed gracefully due to timeout.");
                webSocket.close(NORMAL_CLOSURE_STATUS, "WebSocket closed due to timeout.");
            }
        };
        handler.postDelayed(cleanupRunnable, connectionTimeoutMs);
    }

    /**
     * If there is an existing ongoing timer, this will reset it so that
     * the previously set connectionTimeoutMs can be counted down again.
     */
    synchronized void reset() {
        if (cleanupRunnable != null) {
            handler.removeCallbacks(cleanupRunnable);
            handler.postDelayed(cleanupRunnable, connectionTimeoutMs);
        }
    }

    /**
     * If there is an existing ongoing timer, stop counting it down.
     * The {@see webSocket} provided at {@link #start(WebSocket, long)} will not be closed.
     */
    synchronized void stop() {
        if (cleanupRunnable != null) {
            handler.removeCallbacks(cleanupRunnable);
        }
        cleanupRunnable = null;
        connectionTimeoutMs = -1;
    }
}
