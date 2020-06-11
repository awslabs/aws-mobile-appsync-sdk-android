/*
 * Copyright 2018-2020 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * This package contains generic utilities that ARE NOT part of the AppSync domain.
 * Utilities that have AppSync concerns must find another home, or else this package
 * will take on muddled concerns.
 *
 * {@link com.amazonaws.mobileconnectors.appsync.util.Await} is useful for converting
 * async calls to sync.
 *
 * {@link com.amazonaws.mobileconnectors.appsync.util.Consumer} is a compat version of
 * {@link java.util.function.Consumer}, which can be used without worrying about platform
 * versions.
 *
 * {@link com.amazonaws.mobileconnectors.appsync.util.Sleep} is a wrapper around
 * {@link java.lang.Thread#sleep(long)}, which reduces the boiler plate of use.
 *
 * {@link com.amazonaws.mobileconnectors.appsync.util.Wifi} is for turning on/off the
 * test device's WiFi.
 *
 * {@link com.amazonaws.mobileconnectors.appsync.util.JsonExtract} and
 * {@link com.amazonaws.mobileconnectors.appsync.util.DataFile} are string/file manipulation
 * utilities.
 */
package com.amazonaws.mobileconnectors.appsync.util;
