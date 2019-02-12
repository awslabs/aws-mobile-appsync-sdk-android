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

package com.apollographql.apollo.api.internal;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Contains utility methods for checking Preconditions
 */
public final class Utils {
  private Utils() {
  }

  /**
   * Checks if the object is null. Returns the object if it is not null, else throws a NullPointerException with the
   * error message.
   *
   * @param reference    the object whose nullability has to be checked
   * @param errorMessage the message to use with the NullPointerException
   * @param <T>          the value type
   * @return The object itself
   * @throws NullPointerException if the object is null
   */
  @Nonnull public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  /**
   * Checks if two {@link Set} are disjoint. Returns true if the sets don't have a single common element. Also returns
   * true if either of the sets is null.
   *
   * @param setOne the first set
   * @param setTwo the second set
   * @param <E>    the value type contained within the sets
   * @return True if the sets don't have a single common element or if either of the sets is null.
   */
  public static <E> boolean areDisjoint(Set<E> setOne, Set<E> setTwo) {
    if (setOne == null || setTwo == null) {
      return true;
    }
    Set<E> smallerSet = setOne;
    Set<E> largerSet = setTwo;
    if (setOne.size() > setTwo.size()) {
      smallerSet = setTwo;
      largerSet = setOne;
    }
    for (E el : smallerSet) {
      if (largerSet.contains(el)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the object is null. Returns the object if it is not null, else throws a NullPointerException.
   *
   * @param reference the object whose nullability has to be checked
   * @param <T>       the value type
   * @return The object itself
   * @throws NullPointerException if the object is null
   */
  @Nonnull public static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }
}
