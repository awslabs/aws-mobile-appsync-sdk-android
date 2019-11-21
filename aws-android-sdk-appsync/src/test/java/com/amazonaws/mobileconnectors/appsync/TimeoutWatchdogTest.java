/*
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

import okhttp3.WebSocket;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TimeoutWatchdogTest {

    private WebSocket webSocket;
    private TimeoutWatchdog watchdog;

    @Before
    public void setup() {
        webSocket = mock(WebSocket.class);
        watchdog = new TimeoutWatchdog();
    }

    @Test
    public void webSocketClosesAfterTimeElapsesFromStart() {
        // When watchdog is started,
        watchdog.start(webSocket, 100);

        // Act: the timeout elapses,
        ShadowLooper.idleMainLooper(101, TimeUnit.MILLISECONDS);

        // The WebSocket is closed.
        verify(webSocket).close(eq(1000), anyString());
    }

    @Test
    public void webSocketNotTouchedWhenWatchdogNotStarted() {
        // Arrange: watchdog not started
        // watchdog.start(webSocket, 100);

        // Act: Time elapses
        ShadowLooper.idleMainLooper(101, TimeUnit.MILLISECONDS);

        // WebSocket is not touched
        verifyZeroInteractions(webSocket);
    }

    @Test
    public void webSocketNotClosedAfterResetBeforeNewTimeout() {
        // Arrange: timer is started and almost counted down.
        watchdog.start(webSocket, 100);
        ShadowLooper.idleMainLooper(99, TimeUnit.MILLISECONDS);

        // Act: Timer is reset, and time advances.
        watchdog.reset();
        ShadowLooper.idleMainLooper(99, TimeUnit.MILLISECONDS);

        // Assert: the WebSocket still wasn't closed, even though 198ms have elapsed.
        verifyZeroInteractions(webSocket);
    }

    @Test
    public void webSocketIsClosedEvenAfterResetTimePeriod() {
        // Arrange: started watchdog, time has gone by.
        watchdog.start(webSocket, 100);
        ShadowLooper.idleMainLooper(99, TimeUnit.MILLISECONDS);

        // Act: reset, and then more than the new time goes by
        watchdog.reset();
        ShadowLooper.idleMainLooper(101, TimeUnit.MILLISECONDS);

        // Assert: webSocket is closed
        verify(webSocket).close(eq(1_000), anyString());
    }

    @Test
    public void webSocketNotClosedIfWatchdogStoppedBeforeTimeout() {
        // Arrange: timer is started, and almost out of time
        watchdog.start(webSocket, 100);
        ShadowLooper.idleMainLooper(99, TimeUnit.MILLISECONDS);

        // Act: we stop it, and then much more time (past original quota) elapses
        watchdog.stop();
        ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS);

        // Assert: WebSocket is still open.
        verifyZeroInteractions(webSocket);
    }

    @Test
    public void resetOnStoppedWatchdogDoesNothing() {
        watchdog.reset();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verifyZeroInteractions(webSocket);
    }

    @Test
    public void stopOnStoppedWatchdogDoesNothing() {
        watchdog.stop();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verifyZeroInteractions(webSocket);
    }
}