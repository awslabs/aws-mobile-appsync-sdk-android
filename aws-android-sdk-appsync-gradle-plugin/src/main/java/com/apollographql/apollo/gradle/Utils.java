/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
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
