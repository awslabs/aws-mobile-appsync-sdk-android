/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo;

import com.amazonaws.mobileconnectors.appsync.AppSyncMutationCall;
import com.amazonaws.mobileconnectors.appsync.AppSyncPrefetch;
import com.amazonaws.mobileconnectors.appsync.AppSyncQueryCall;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.GraphQLStoreOperation;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.ApolloCallTracker;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.internal.RealAppSyncSubscriptionCall;
import com.apollographql.apollo.internal.RealAppSyncCall;
import com.apollographql.apollo.internal.RealAppSyncPrefetch;
import com.apollographql.apollo.internal.ResponseFieldMapperFactory;
import com.apollographql.apollo.internal.cache.normalized.RealAppSyncStore;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;
import com.apollographql.apollo.internal.subscription.NoOpSubscriptionManager;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloClient class represents the abstraction for the graphQL client that will be used to execute queries and read
 * the responses back.
 *
 * <h3>ApolloClient should be shared</h3>
 *
 * Since each ApolloClient holds its own connection pool and thread pool, it is recommended to only create a single
 * ApolloClient and use that for execution of all the queries, as this would reduce latency and would also save memory.
 * Conversely, creating a client for each query execution would result in resource wastage on idle pools.
 *
 *
 * <p>See the {@link Builder} class for configuring the ApolloClient.
 */
public final class ApolloClient
    implements AppSyncQueryCall.Factory, AppSyncMutationCall.Factory, AppSyncSubscriptionCall.Factory, AppSyncPrefetch.Factory {

  public static Builder builder() {
    return new Builder();
  }

  private final HttpUrl serverUrl;
  private final Call.Factory httpCallFactory;
  private final HttpCache httpCache;
  private final ApolloStore mApolloStore;
  private final ScalarTypeAdapters scalarTypeAdapters;
  private final ResponseFieldMapperFactory responseFieldMapperFactory = new ResponseFieldMapperFactory();
  private final Executor dispatcher;
  private final HttpCachePolicy.Policy defaultHttpCachePolicy;
  private final ResponseFetcher defaultResponseFetcher;
  private final CacheHeaders defaultCacheHeaders;
  private final ApolloLogger logger;
  private final ApolloCallTracker tracker = new ApolloCallTracker();
  private final List<ApolloInterceptor> applicationInterceptors;
  private final boolean sendOperationIdentifiers;
  private final SubscriptionManager subscriptionManager;

  private ApolloClient(HttpUrl serverUrl,
      Call.Factory httpCallFactory,
      HttpCache httpCache,
      ApolloStore apolloStore,
      ScalarTypeAdapters scalarTypeAdapters,
      Executor dispatcher,
      HttpCachePolicy.Policy defaultHttpCachePolicy,
      ResponseFetcher defaultResponseFetcher,
      CacheHeaders defaultCacheHeaders,
      ApolloLogger logger,
      List<ApolloInterceptor> applicationInterceptors,
      boolean sendOperationIdentifiers,
      SubscriptionManager subscriptionManager) {
    this.serverUrl = serverUrl;
    this.httpCallFactory = httpCallFactory;
    this.httpCache = httpCache;
    this.mApolloStore = apolloStore;
    this.scalarTypeAdapters = scalarTypeAdapters;
    this.dispatcher = dispatcher;
    this.defaultHttpCachePolicy = defaultHttpCachePolicy;
    this.defaultResponseFetcher = defaultResponseFetcher;
    this.defaultCacheHeaders = defaultCacheHeaders;
    this.logger = logger;
    this.applicationInterceptors = applicationInterceptors;
    this.sendOperationIdentifiers = sendOperationIdentifiers;
    this.subscriptionManager = subscriptionManager;
  }

  @Override
  public <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(
      @Nonnull Mutation<D, T, V> mutation) {
    return newCall(mutation).responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY);
  }

  @Override
  public <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(
      @Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates) {
    checkNotNull(withOptimisticUpdates, "withOptimisticUpdate == null");
    return newCall(mutation).toBuilder().responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
        .optimisticUpdates(Optional.<Operation.Data>fromNullable(withOptimisticUpdates)).build();
  }

  @Override
  public <D extends Query.Data, T, V extends Query.Variables> AppSyncQueryCall<T> query(@Nonnull Query<D, T, V> query) {
    return newCall(query);
  }

  @Override
  public <D extends Subscription.Data, T, V extends Subscription.Variables> AppSyncSubscriptionCall<T> subscribe(
      @Nonnull Subscription<D, T, V> subscription) {
        return new RealAppSyncSubscriptionCall<T>(subscription, subscriptionManager, this, this.logger, newCall(subscription));
  }

  /**
   * Prepares the {@link AppSyncPrefetch} which will be executed at some point in the future.
   */
  @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> AppSyncPrefetch prefetch(
      @Nonnull Operation<D, T, V> operation) {
    return new RealAppSyncPrefetch(operation, serverUrl, httpCallFactory, scalarTypeAdapters, dispatcher, logger,
        tracker, sendOperationIdentifiers);
  }

  /**
   * @return The default {@link CacheHeaders} which this instance of {@link ApolloClient} was configured.
   */
  public CacheHeaders defaultCacheHeaders() {
    return defaultCacheHeaders;
  }

  /**
   * Clear all entries from the {@link HttpCache}, if present.
   */
  public void clearHttpCache() {
    if (httpCache != null) {
      httpCache.clear();
    }
  }

  /**
   * Clear all entries from the normalized cache.
   *
   * @return {@link GraphQLStoreOperation} operation to execute
   */
  public @Nonnull
  GraphQLStoreOperation<Boolean> clearNormalizedCache() {
    return mApolloStore.clearAll();
  }

  /**
   * @return The {@link ApolloStore} managing access to the normalized cache created by {@link
   * Builder#normalizedCache(NormalizedCacheFactory, CacheKeyResolver)}  }
   */
  public ApolloStore apolloStore() {
    return mApolloStore;
  }

  /**
   * Sets the idleResourceCallback which will be called when this ApolloClient is idle.
   */
  public void idleCallback(IdleResourceCallback idleResourceCallback) {
    tracker.setIdleResourceCallback(idleResourceCallback);
  }

  /**
   * Returns the count of {@link GraphQLCall} & {@link AppSyncPrefetch} objects which are currently in progress.
   */
  public int activeCallsCount() {
    return tracker.activeCallsCount();
  }

  Response cachedHttpResponse(String cacheKey) throws IOException {
    if (httpCache != null) {
      return httpCache.read(cacheKey);
    } else {
      return null;
    }
  }

  private <D extends Operation.Data, T, V extends Operation.Variables> RealAppSyncCall<T> newCall(
      @Nonnull Operation<D, T, V> operation) {
    return RealAppSyncCall.<T>builder()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(defaultHttpCachePolicy)
        .responseFieldMapperFactory(responseFieldMapperFactory)
        .scalarTypeAdapters(scalarTypeAdapters)
        .apolloStore(mApolloStore)
        .responseFetcher(defaultResponseFetcher)
        .cacheHeaders(defaultCacheHeaders)
        .dispatcher(dispatcher)
        .logger(logger)
        .applicationInterceptors(applicationInterceptors)
        .tracker(tracker)
        .refetchQueries(Collections.<Query>emptyList())
        .refetchQueryNames(Collections.<OperationName>emptyList())
        .sendOperationIdentifiers(sendOperationIdentifiers)
        .subscriptionManager(subscriptionManager)
        .build();
  }

  @SuppressWarnings("WeakerAccess") public static class Builder {
    Call.Factory callFactory;
    HttpUrl serverUrl;
    HttpCache httpCache;
    ApolloStore mApolloStore = ApolloStore.NO_APOLLO_STORE;
    Optional<NormalizedCacheFactory> cacheFactory = Optional.absent();
    Optional<CacheKeyResolver> cacheKeyResolver = Optional.absent();
    HttpCachePolicy.Policy defaultHttpCachePolicy = HttpCachePolicy.NETWORK_ONLY;
    ResponseFetcher defaultResponseFetcher = AppSyncResponseFetchers.CACHE_FIRST;
    CacheHeaders defaultCacheHeaders = CacheHeaders.NONE;
    final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
    Executor dispatcher;
    Optional<Logger> logger = Optional.absent();
    final List<ApolloInterceptor> applicationInterceptors = new ArrayList<>();
    boolean sendOperationIdentifiers;
    SubscriptionManager subscriptionManager = new NoOpSubscriptionManager();

    private Builder() {
    }

    /**
     * Set the {@link OkHttpClient} to use for making network requests.
     *
     * @param okHttpClient the client to use.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder okHttpClient(@Nonnull OkHttpClient okHttpClient) {
      return callFactory(checkNotNull(okHttpClient, "okHttpClient is null"));
    }

    /**
     * Set the custom call factory for creating {@link Call} instances. <p> Note: Calling {@link
     * #okHttpClient(OkHttpClient)} automatically sets this value.
     */
    public Builder callFactory(@Nonnull Call.Factory factory) {
      this.callFactory = checkNotNull(factory, "factory == null");
      return this;
    }

    /**
     * <p>Set the API server's base url.</p>
     *
     * @param serverUrl the url to set.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder serverUrl(@Nonnull HttpUrl serverUrl) {
      this.serverUrl = checkNotNull(serverUrl, "serverUrl is null");
      return this;
    }

    /**
     * <p>Set the API server's base url.</p>
     *
     * @param serverUrl the url to set.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder serverUrl(@Nonnull String serverUrl) {
      this.serverUrl = HttpUrl.parse(checkNotNull(serverUrl, "serverUrl == null"));
      return this;
    }

    /**
     * Set the configuration to be used for request/response http cache.
     *
     * @param httpCache The to use for reading and writing cached response.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder httpCache(@Nonnull HttpCache httpCache) {
      this.httpCache = checkNotNull(httpCache, "httpCache == null");
      return this;
    }

    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the {@link NormalizedCacheFactory} used to construct a {@link NormalizedCache}.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder normalizedCache(@Nonnull NormalizedCacheFactory normalizedCacheFactory) {
      return normalizedCache(normalizedCacheFactory, CacheKeyResolver.DEFAULT);
    }

    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the {@link NormalizedCacheFactory} used to construct a {@link NormalizedCache}.
     * @param keyResolver            the {@link CacheKeyResolver} to use to normalize records
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder normalizedCache(@Nonnull NormalizedCacheFactory normalizedCacheFactory,
        @Nonnull CacheKeyResolver keyResolver) {
      cacheFactory = Optional.fromNullable(checkNotNull(normalizedCacheFactory, "normalizedCacheFactory == null"));
      cacheKeyResolver = Optional.fromNullable(checkNotNull(keyResolver, "cacheKeyResolver == null"));
      return this;
    }

    /**
     * Set the type adapter to use for serializing and de-serializing custom GraphQL scalar types.
     *
     * @param scalarType        the scalar type to serialize/deserialize
     * @param customTypeAdapter the type adapter to use
     * @param <T>               the value type
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public <T> Builder addCustomTypeAdapter(@Nonnull ScalarType scalarType,
        @Nonnull final CustomTypeAdapter<T> customTypeAdapter) {
      customTypeAdapters.put(scalarType, customTypeAdapter);
      return this;
    }

    /**
     * The #{@link Executor} to use for dispatching the requests.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder dispatcher(@Nonnull Executor dispatcher) {
      this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
      return this;
    }

    /**
     * Sets the http cache policy to be used as default for all GraphQL {@link Query} operations. Will be ignored for
     * any {@link Mutation} operations. By default http cache policy is set to {@link HttpCachePolicy#NETWORK_ONLY}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultHttpCachePolicy(@Nonnull HttpCachePolicy.Policy cachePolicy) {
      this.defaultHttpCachePolicy = checkNotNull(cachePolicy, "cachePolicy == null");
      return this;
    }

    /**
     * Set the default {@link CacheHeaders} strategy that will be used in each new {@link GraphQLCall}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultCacheHeaders(@Nonnull CacheHeaders cacheHeaders) {
      this.defaultCacheHeaders = checkNotNull(cacheHeaders, "cacheHeaders == null");
      return this;
    }

    /**
     * Set the default {@link ResponseFetcher} to be used with each new {@link GraphQLCall}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultResponseFetcher(@Nonnull ResponseFetcher defaultResponseFetcher) {
      this.defaultResponseFetcher = checkNotNull(defaultResponseFetcher, "defaultResponseFetcher == null");
      return this;
    }

    /**
     * The {@link Logger} to use for logging purposes.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder logger(@Nullable Logger logger) {
      this.logger = Optional.fromNullable(logger);
      return this;
    }

    /**
     * <p>Adds an interceptor that observes the full span of each call: from before the connection is established until
     * after the response source is selected (either the server, cache or both). This method can be called multiple
     * times for adding multiple application interceptors. </p>
     *
     * <p>Note: Interceptors will be called <b>in the order in which they are added to the list of interceptors</b> and
     * if any of the interceptors tries to short circuit the responses, then subsequent interceptors <b>won't</b> be
     * called.</p>
     *
     * @param interceptor Application level interceptor to add
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder addApplicationInterceptor(@Nonnull ApolloInterceptor interceptor) {
      applicationInterceptors.add(interceptor);
      return this;
    }

    /**
     * @param sendOperationIdentifiers True if ApolloClient should send a operation identifier instead of the operation
     *                                 definition. Default: false.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder sendOperationIdentifiers(boolean sendOperationIdentifiers) {
      this.sendOperationIdentifiers = sendOperationIdentifiers;
      return this;
    }

    public Builder subscriptionManager(@Nonnull SubscriptionManager subscriptionManager) {
      this.subscriptionManager = subscriptionManager;
      return this;
    }

    /**
     * Builds the {@link ApolloClient} instance using the configured values.
     *
     * Note that if the {@link #dispatcher} is not called, then a default {@link Executor} is used.
     *
     * @return The configured {@link ApolloClient}
     */
    public ApolloClient build() {
      checkNotNull(serverUrl, "serverUrl is null");

      ApolloLogger apolloLogger = new ApolloLogger(logger);

      Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

      HttpCache httpCache = this.httpCache;
      if (httpCache != null) {
        callFactory = addHttCacheInterceptorIfNeeded(callFactory, httpCache.interceptor());
      }

      Executor dispatcher = this.dispatcher;
      if (dispatcher == null) {
        dispatcher = defaultDispatcher();
      }

      ScalarTypeAdapters scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);

      ApolloStore apolloStore = this.mApolloStore;
      Optional<NormalizedCacheFactory> cacheFactory = this.cacheFactory;
      Optional<CacheKeyResolver> cacheKeyResolver = this.cacheKeyResolver;
      if (cacheFactory.isPresent() && cacheKeyResolver.isPresent()) {
        final NormalizedCache normalizedCache = cacheFactory.get().createChain(RecordFieldJsonAdapter.create());
        apolloStore = new RealAppSyncStore(normalizedCache, cacheKeyResolver.get(), scalarTypeAdapters, dispatcher,
                apolloLogger);
      }

      return new ApolloClient(serverUrl,
          callFactory,
          httpCache,
              apolloStore,
          scalarTypeAdapters,
          dispatcher,
          defaultHttpCachePolicy,
          defaultResponseFetcher,
          defaultCacheHeaders,
              apolloLogger,
          applicationInterceptors,
          sendOperationIdentifiers,
          subscriptionManager);
    }

    private Executor defaultDispatcher() {
      return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), new ThreadFactory() {
        @Override public Thread newThread(@Nonnull Runnable runnable) {
          return new Thread(runnable, "Apollo Dispatcher");
        }
      });
    }

    private static Call.Factory addHttCacheInterceptorIfNeeded(Call.Factory callFactory,
        Interceptor httCacheInterceptor) {
      if (callFactory instanceof OkHttpClient) {
        OkHttpClient client = (OkHttpClient) callFactory;
        for (Interceptor interceptor : client.interceptors()) {
          if (interceptor.getClass().equals(httCacheInterceptor.getClass())) {
            return callFactory;
          }
        }
        return client.newBuilder().addInterceptor(httCacheInterceptor).build();
      }
      return callFactory;
    }
  }
}
