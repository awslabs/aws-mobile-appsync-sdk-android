/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * RealApolloInterceptorChain is responsible for building the entire interceptor chain. Based on the task at hand, the
 * chain may contain interceptors which fetch responses from the normalized cache, make network calls to fetch data if
 * needed or parse the http responses to inflate models.
 */
public final class RealApolloInterceptorChain implements ApolloInterceptorChain {
  private final List<ApolloInterceptor> interceptors;
  private final int interceptorIndex;

  public RealApolloInterceptorChain(@Nonnull List<ApolloInterceptor> interceptors) {
    this(interceptors, 0);
  }

  private RealApolloInterceptorChain(List<ApolloInterceptor> interceptors, int interceptorIndex) {
    if (interceptorIndex > interceptors.size()) throw new IllegalArgumentException();
    this.interceptors = new ArrayList<>(checkNotNull(interceptors, "interceptors == null"));
    this.interceptorIndex = interceptorIndex;
  }

  @Override public void proceedAsync(@Nonnull ApolloInterceptor.InterceptorRequest request,
      @Nonnull Executor dispatcher, @Nonnull ApolloInterceptor.CallBack callBack) {
    if (interceptorIndex >= interceptors.size()) throw new IllegalStateException();
    interceptors.get(interceptorIndex).interceptAsync(request, new RealApolloInterceptorChain(interceptors,
        interceptorIndex + 1), dispatcher, callBack);
  }

  @Override public void dispose() {
    for (ApolloInterceptor interceptor : interceptors) {
      interceptor.dispose();
    }
  }
}
