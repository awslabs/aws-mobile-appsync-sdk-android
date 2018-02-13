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
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.cache.normalized.AppSyncStore;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.CustomTypeAdapter;
import com.amazonaws.mobileconnectors.appsync.cache.normalized.sql.AppSyncSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.AppSyncSigV4SignerInterceptor;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class AWSAppSyncClient {
    private static final String defaultSqlStoreName = "appsyncstore";
    private static final String defaultMutationSqlStoreName = "appsyncstore_mutation";
    ApolloClient mApolloClient;
    AppSyncStore mSyncStore;

    private AWSAppSyncClient(AWSAppSyncClient.Builder builder) {
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

        OkHttpClient okHttpClient = builder.mOkHttpClient;

        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(appSyncSigV4SignerInterceptor)
                    .build();
        }

        AppSyncMutationsSqlHelper mutationsSqlHelper = new AppSyncMutationsSqlHelper(builder.mContext, defaultMutationSqlStoreName);
        AppSyncMutationSqlCacheOperations sqlCacheOperations = new AppSyncMutationSqlCacheOperations(mutationsSqlHelper);

        AppSyncOptimisticUpdateInterceptor optimisticUpdateInterceptor = new AppSyncOptimisticUpdateInterceptor();

        AppSyncCustomNetworkInvoker networkInvoker =
                new AppSyncCustomNetworkInvoker(HttpUrl.parse(builder.mServerUrl),
                        okHttpClient,
                        new ScalarTypeAdapters(builder.customTypeAdapters),
                        builder.mPersistentMutationsCallback);

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
                        builder.mContext))
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

        mApolloClient = clientBuilder.build();

        mSyncStore = new AppSyncStore(mApolloClient.apolloStore());

        optimisticUpdateInterceptor.setStore(mApolloClient.apolloStore());

    }

    public static class Builder {
        // AWS
        Regions mRegion;
        AWSCredentialsProvider mCredentialsProvider;
        APIKeyAuthProvider mApiKey;
        CognitoUserPoolsAuthProvider mCognitoUserPoolsAuthProvider;
        NormalizedCacheFactory mNormalizedCacheFactory;
        CacheKeyResolver mResolver;

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

        public Builder normalizedCache(NormalizedCacheFactory normalizedCacheFactory) {
            mNormalizedCacheFactory = normalizedCacheFactory;
            return this;
        }

        public Builder resolver(CacheKeyResolver resolver) {
            mResolver = resolver;
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
                        String id = (String) recordSet.get("id");
                        if (id == null || id.isEmpty()) {
                            return CacheKey.NO_KEY;
                        }
                        return CacheKey.from(id);
                    }

                    @Nonnull
                    @Override
                    public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {

                        return null;
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
        return mApolloClient.mutate(mutation);
    }

    public <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates) {
        return mApolloClient.mutate(mutation, withOptimisticUpdates);
    }

    public AppSyncStore getStore() {
        return mSyncStore;
    }
}
