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


import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public class Functions {
  /**
   * Returns the identity function.
   */
  // implementation is "fully variant"; E has become a "pass-through" type
  public static <E> Function<E, E> identity() {
    return (Function<E, E>) IdentityFunction.INSTANCE;
  }

  // enum singleton pattern
  private enum IdentityFunction implements Function<Object, Object> {
    INSTANCE;

    @Override
    @Nullable
    public Object apply(@Nullable Object o) {
      return o;
    }

    @Override
    public String toString() {
      return "Functions.identity()";
    }
  }

  /**
   * Returns a function that calls {@code toString()} on its argument. The function does not accept
   * nulls; it will throw a {@link NullPointerException} when applied to {@code null}.
   *
   * <p><b>Warning:</b> The returned function may not be <i>consistent with equals</i> (as
   * documented at {@link Function#apply}). For example, this function yields different results for
   * the two equal instances {@code ImmutableSet.of(1, 2)} and {@code ImmutableSet.of(2, 1)}.
   */
  public static Function<Object, String> toStringFunction() {
    return ToStringFunction.INSTANCE;
  }

  // enum singleton pattern
  private enum ToStringFunction implements Function<Object, String> {
    INSTANCE;

    @Override
    public String apply(Object o) {
      checkNotNull(o); // eager for GWT.
      return o.toString();
    }

    @Override
    public String toString() {
      return "Functions.toStringFunction()";
    }
  }
}
