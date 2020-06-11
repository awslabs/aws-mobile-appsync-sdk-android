/*
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.util;

import android.content.Context;
import android.net.wifi.WifiManager;

import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.support.test.InstrumentationRegistry.getTargetContext;

public final class Wifi {
    private Wifi() {}

    public static void turnOn() {
        RetryStrategies.linear(() -> wifiManager().setWifiEnabled(true), Wifi::isOn, 3, 2);
    }

    public static void turnOff() {
        RetryStrategies.linear(() -> wifiManager().setWifiEnabled(false), Wifi::isOff, 3, 2);
    }

    static boolean isOff() {
        return !isOn();
    }

    static boolean isOn() {
        return WIFI_STATE_ENABLED == wifiManager().getWifiState();
    }

    private static WifiManager wifiManager() {
        return (WifiManager) getTargetContext().getSystemService(Context.WIFI_SERVICE);
    }
}
