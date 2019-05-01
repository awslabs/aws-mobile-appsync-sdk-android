/**
 * Copyright 2019-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.utils;

import okio.Buffer;

public class StringUtils {
    public static String toHumanReadableAscii(String s) {
        for (int i = 0, length = s.length(), c; i < length; i += Character.charCount(c)) {
            c = s.codePointAt(i);
            if (c > '\u001f' && c < '\u007f') continue;

            Buffer buffer = new Buffer();
            buffer.writeUtf8(s, 0, i);
            for (int j = i; j < length; j += Character.charCount(c)) {
                c = s.codePointAt(j);
                if (c > '\u001f' && c < '\u007f') {
                    buffer.writeUtf8CodePoint(c);
                }
            }
            return buffer.readUtf8();
        }
        return s;
    }
}
