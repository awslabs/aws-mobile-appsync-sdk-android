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

package com.amazonaws.mobileconnectors.appsync.utils;

import android.os.Build;

import com.amazonaws.http.TLS12SocketFactory;

import okhttp3.OkHttpClient;

/**
 * Although this has public access, it is intended for internal use and should not be used directly by host
 * applications. The behavior of this may change without warning.
 */
public class TLS12OkHttpHelper {

    @SuppressWarnings("depreciation")
    public static OkHttpClient.Builder fixTLSPre22(OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                TLS12SocketFactory tls12SocketFactory = TLS12SocketFactory.createTLS12SocketFactory();
                if (tls12SocketFactory != null) {
                    builder.sslSocketFactory(tls12SocketFactory);
                }
            } catch (Exception e) {

            }
        }
        return builder;
    }
}
