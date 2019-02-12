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

package com.apollographql.apollo.internal.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple stack data structure which accepts null elements. Backed by list.
 * @param <E>
 */
public class SimpleStack<E> {

  private List<E> backing;

  public SimpleStack() {
    backing = new ArrayList<>();
  }

  public SimpleStack(int initialSize) {
    backing = new ArrayList<>(initialSize);
  }

  public void push(E element) {
    backing.add(element);
  }

  public E pop() {
    if (isEmpty()) {
      throw new IllegalStateException("Stack is empty.");
    }
    return backing.remove(backing.size() - 1);
  }

  public boolean isEmpty() {
    return backing.isEmpty();
  }
}
