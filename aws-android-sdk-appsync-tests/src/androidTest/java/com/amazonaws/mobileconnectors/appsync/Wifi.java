/*
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.net.wifi.WifiManager;

import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
final class Wifi {
    private Wifi() {}

    static void turnOn() {
        assertTrue(wifiManager().setWifiEnabled(true));
    }

    static void turnOff() {
        assertTrue(wifiManager().setWifiEnabled(false));
    }

    static boolean isOn() {
        return WIFI_STATE_ENABLED != wifiManager().getWifiState();
    }

    private static WifiManager wifiManager() {
        return (WifiManager) getTargetContext().getSystemService(Context.WIFI_SERVICE);
    }
}
