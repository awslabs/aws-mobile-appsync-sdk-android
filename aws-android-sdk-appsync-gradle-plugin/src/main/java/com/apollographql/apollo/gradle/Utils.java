/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.gradle;

import java.io.File;

public class Utils {
  static String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  static boolean isNullOrEmpty(String s) {
    return s == null || s.length() == 0;
  }

  static void deleteDirectory(File directory) {
    if (directory.exists()){
      File[] files = directory.listFiles();
      if (files != null){
        for (File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            file.delete();
          }
        }
      }
    }
    directory.delete();
  }
}
