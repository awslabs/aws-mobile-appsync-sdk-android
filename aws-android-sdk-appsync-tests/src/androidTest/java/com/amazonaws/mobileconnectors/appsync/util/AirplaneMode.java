/*
 * Copyright 2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.mobileconnectors.appsync.util;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

public final class AirplaneMode {
    private static final int TIMEOUT_MS = 500;

    private AirplaneMode() {}

    public static void enable() {
        setAirplaneMode(true);
    }

    public static  void disable() {
        setAirplaneMode(false);
    }

    public static boolean isEnabled() {
        ContentResolver contentResolver = getInstrumentation().getContext().getContentResolver();
        int status = Settings.System.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
        return status == 1;
    }

    private static void setAirplaneMode(boolean shouldEnable) {
        if (shouldEnable == isEnabled()) return;

        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.openQuickSettings();

        BySelector description = By.desc("Airplane mode");
        device.wait(Until.hasObject(description), TIMEOUT_MS);
        device.findObject(description).click();

        getInstrumentation().getContext()
            .sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }
}
