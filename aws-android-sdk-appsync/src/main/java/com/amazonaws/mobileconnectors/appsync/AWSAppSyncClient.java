/**
 * Copyright 2018-2018 Amazon.com,
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

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobileconnectors.appsync.cache.normalized.AppSyncStore;
import com.amazonaws.mobileconnectors.appsync.cache.normalized.sql.AppSyncSqlHelper;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.api.S3ObjectManager;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.AppSyncSigV4SignerInterceptor;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
import com.amazonaws.mobileconnectors.appsync.subscription.RealSubscriptionManager;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class AWSAppSyncClient {
    private static final String defaultSqlStoreName = "appsyncstore";
    private static final String defaultMutationSqlStoreName = "appsyncstore_mutation";
    ApolloClient mApolloClient;
    AppSyncStore mSyncStore;
    private Context applicationContext;
    S3ObjectManager mS3ObjectManager;
    Map<Mutation, MutationInformation> mutationMap;

    private AWSAppSyncClient(AWSAppSyncClient.Builder builder) {
        applicationContext = builder.mContext.getApplicationContext();

        AppSyncSigV4SignerInterceptor appSyncSigV4SignerInterceptor = null;
        if (builder.mCredentialsProvider != null) {
            appSyncSigV4SignerInterceptor = new AppSyncSigV4SignerInterceptor(builder.mCredentialsProvider, builder.mRegion.getName());
        } else if (builder.mCognitoUserPoolsAuthProvider != null) {
            appSyncSigV4SignerInterceptor = new AppSyncSigV4SignerInterceptor(builder.mCognitoUserPoolsAuthProvider, builder.mRegion.getName());
        } else if (builder.mApiKey != null) {
            appSyncSigV4SignerInterceptor = new AppSyncSigV4SignerInterceptor(builder.mApiKey, builder.mRegion.getName());
        } else {
            throw new RuntimeException("Client requires credentials. Please use #apiKey() #credentialsProvider() or #cognitoUserPoolsAuthProvider() to set the credentials.");
        }

        OkHttpClient.Builder okHttpClientBuilder;
        if (builder.mOkHttpClient == null) {
            okHttpClientBuilder = new OkHttpClient.Builder();
        } else {
            okHttpClientBuilder = builder.mOkHttpClient.newBuilder();
        }

        OkHttpClient okHttpClient = okHttpClientBuilder
                .addInterceptor(appSyncSigV4SignerInterceptor)
                .build();

        AppSyncMutationsSqlHelper mutationsSqlHelper = new AppSyncMutationsSqlHelper(builder.mContext, defaultMutationSqlStoreName);
        AppSyncMutationSqlCacheOperations sqlCacheOperations = new AppSyncMutationSqlCacheOperations(mutationsSqlHelper);
        mutationMap = new HashMap<>();

        AppSyncOptimisticUpdateInterceptor optimisticUpdateInterceptor = new AppSyncOptimisticUpdateInterceptor();

        AppSyncCustomNetworkInvoker networkInvoker =
                new AppSyncCustomNetworkInvoker(HttpUrl.parse(builder.mServerUrl),
                        okHttpClient,
                        new ScalarTypeAdapters(builder.customTypeAdapters),
                        builder.mPersistentMutationsCallback,
                        builder.mS3ObjectManager);

        ApolloClient.Builder clientBuilder = ApolloClient.builder()
                .serverUrl(builder.mServerUrl)
                .normalizedCache(builder.mNormalizedCacheFactory, builder.mResolver)
                .addApplicationInterceptor(optimisticUpdateInterceptor)
                .addApplicationInterceptor(new AppSyncOfflineMutationInterceptor(
                        new AppSyncOfflineMutationManager(builder.mContext,
                                builder.customTypeAdapters,
                                sqlCacheOperations,
                                networkInvoker),
                        false,
                        builder.mContext,
                        mutationMap,
                        this,
                        builder.mConflictResolver))
                .addApplicationInterceptor(new AppSyncComplexObjectsInterceptor(builder.mS3ObjectManager))
                .okHttpClient(okHttpClient);

        for (ScalarType scalarType : builder.customTypeAdapters.keySet()) {
            clientBuilder.addCustomTypeAdapter(scalarType, builder.customTypeAdapters.get(scalarType));
        }

        if (builder.mDispatcher != null) {
            clientBuilder.dispatcher(builder.mDispatcher);
        }

        if (builder.mCacheHeaders != null) {
            clientBuilder.defaultCacheHeaders(builder.mCacheHeaders);
        }

        if (builder.mDefaultResponseFetcher != null) {
            clientBuilder.defaultResponseFetcher(builder.mDefaultResponseFetcher);
        }

        SubscriptionManager subscriptionManager = new RealSubscriptionManager(builder.mContext.getApplicationContext());
        clientBuilder.subscriptionManager(subscriptionManager);

        mApolloClient = clientBuilder.build();

        mSyncStore = new AppSyncStore(mApolloClient.apolloStore());

        optimisticUpdateInterceptor.setStore(mApolloClient.apolloStore());
        subscriptionManager.setStore(mApolloClient.apolloStore());
        subscriptionManager.setScalarTypeAdapters(new ScalarTypeAdapters(builder.customTypeAdapters));
        mS3ObjectManager = builder.mS3ObjectManager;
    }

    public static class Builder {
        // AWS
        Regions mRegion;
        AWSCredentialsProvider mCredentialsProvider;
        APIKeyAuthProvider mApiKey;
        CognitoUserPoolsAuthProvider mCognitoUserPoolsAuthProvider;
        NormalizedCacheFactory mNormalizedCacheFactory;
        CacheKeyResolver mResolver;
        ConflictResolverInterface mConflictResolver;

        // Apollo
        String mServerUrl;
        final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
        Executor mDispatcher;
        OkHttpClient mOkHttpClient;
        ResponseFetcher mDefaultResponseFetcher = AppSyncResponseFetchers.CACHE_FIRST;
        CacheHeaders mCacheHeaders;
        PersistentMutationsCallback mPersistentMutationsCallback;

        // Android
        Context mContext;
        S3ObjectManager mS3ObjectManager;

        private Builder() { }

        public Builder region(Regions region) {
            mRegion = region;
            return this;
        }

        public Builder credentialsProvider(AWSCredentialsProvider credentialsProvider) {
            mCredentialsProvider = credentialsProvider;
            return this;
        }

        public Builder apiKey(APIKeyAuthProvider apiKey) {
            mApiKey = apiKey;
            return this;
        }

        public Builder cognitoUserPoolsAuthProvider(CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider) {
            mCognitoUserPoolsAuthProvider = cognitoUserPoolsAuthProvider;
            return this;
        }

        public Builder serverUrl(String serverUrl) {
            mServerUrl = serverUrl;
            return this;
        }

        public Builder context(Context context) {
            mContext = context;
            return this;
        }

        public Builder s3ObjectManager(S3ObjectManager s3ObjectManager) {
            mS3ObjectManager = s3ObjectManager;
            return this;
        }

        public Builder normalizedCache(NormalizedCacheFactory normalizedCacheFactory) {
            mNormalizedCacheFactory = normalizedCacheFactory;
            return this;
        }

        public Builder resolver(CacheKeyResolver resolver) {
            mResolver = resolver;
            return this;
        }

        public Builder conflictResolver(ConflictResolverInterface conflictResolver) {
            mConflictResolver = conflictResolver;
            return this;
        }

        public <T> Builder addCustomTypeAdapter(ScalarType scalarType,
                                            final CustomTypeAdapter<T> customTypeAdapter) {
            customTypeAdapters.put(scalarType, customTypeAdapter);
            return this;
        }

        public Builder dispatcher(Executor dispatcher) {
            mDispatcher = dispatcher;
            return this;
        }

        public Builder defaultCacheHeaders(CacheHeaders cacheHeaders) {
            mCacheHeaders = cacheHeaders;
            return this;
        }

        public Builder okHttpClient(OkHttpClient okHttpClient) {
            mOkHttpClient = okHttpClient;
            return this;
        }

        public Builder defaultResponseFetcher(ResponseFetcher defaultResponseFetcher) {
            mDefaultResponseFetcher = defaultResponseFetcher;
            return this;
        }

        public Builder persistentMutationsCallback(PersistentMutationsCallback persistentMutationsCallback) {
            mPersistentMutationsCallback = persistentMutationsCallback;
            return this;
        }

        public AWSAppSyncClient build() {
            if (mNormalizedCacheFactory == null) {
                AppSyncSqlHelper appSyncSqlHelper = AppSyncSqlHelper.create(mContext, defaultSqlStoreName);

                //Create NormalizedCacheFactory
                mNormalizedCacheFactory = new SqlNormalizedCacheFactory(appSyncSqlHelper);
            }

            if (mResolver == null) {
                mResolver = new CacheKeyResolver() {
                    @Nonnull
                    @Override
                    public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
                        return formatCacheKey((String) recordSet.get("id"));
                    }

                    @Nonnull
                    @Override
                    public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {

                        return formatCacheKey((String) field.resolveArgument("id", variables));
                    }

                    private CacheKey formatCacheKey(String id) {
                        if (id == null || id.isEmpty()) {
                            return CacheKey.NO_KEY;
                        } else {
                            return CacheKey.from(id);
                        }
                    }
                };
            }

            return new AWSAppSyncClient(this);
        }
    }

    /**
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    public <D extends Query.Data, T, V extends Query.Variables> AppSyncQueryCall<T> query(@Nonnull Query<D, T, V> query) {
        return mApolloClient.query(query);
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation) {
        return mutate(mutation, false);
    }

    protected <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, boolean isRetry) {
        if (isRetry) {
            mutationMap.put(mutation, null);
        }
        return mApolloClient.mutate(mutation);
    }

    protected <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates, boolean isRetry) {
        if (isRetry) {
            mutationMap.put(mutation, null);
        }
        return mApolloClient.mutate(mutation, withOptimisticUpdates);
    }


    public <D extends Subscription.Data, T, V extends Subscription.Variables> AppSyncSubscriptionCall<T> subscribe(@Nonnull Subscription<D, T, V> subscription) {
        return mApolloClient.subscribe(subscription);
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates) {
        return mutate(mutation, withOptimisticUpdates, false);

    }

    public AppSyncStore getStore() {
        return mSyncStore;
    }

    public S3ObjectManager getS3ObjectManager() {
        return mS3ObjectManager;
    }
}
