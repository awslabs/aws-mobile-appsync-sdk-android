/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.cache.normalized.AppSyncStore;
import com.amazonaws.mobileconnectors.appsync.cache.normalized.sql.AppSyncSqlHelper;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.retry.RetryInterceptor;
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.AppSyncSigV4SignerInterceptor;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.OidcAuthProvider;
import com.amazonaws.mobileconnectors.appsync.subscription.RealSubscriptionManager;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringUtils;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.S3ObjectManager;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;
import com.apollographql.apollo.internal.util.Cancelable;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class AWSAppSyncClient {
    /** Name of the default client database (Query Cache) that stores the query responses. */
    static final String DEFAULT_QUERY_SQL_STORE_NAME = "appsyncstore";

    /** Name of the default client database (Mutation Queue) that stores the persistent mutations. */
    static final String DEFAULT_MUTATION_SQL_STORE_NAME = "appsyncstore_mutation";

    /** Name of the client database (Subscriptions Metadata) that stores the LastSyncTime used by Delta Sync. */
    static final String DEFAULT_DELTA_SYNC_SQL_STORE_NAME = "appsync_deltasync_db";

    /** Delimiter that is used in the client database (cache) names. */
    static final String DATABASE_NAME_DELIMITER = "_";

    private static final String TAG = AWSAppSyncClient.class.getSimpleName();

    ApolloClient mApolloClient;
    AppSyncStore mSyncStore;
    private Context applicationContext;
    S3ObjectManager mS3ObjectManager;

    //Map that houses retried mutations.
    private Map<Mutation, MutationInformation> mutationsToRetryAfterConflictResolution;
    private AppSyncOfflineMutationManager mAppSyncOfflineMutationManager = null;

    String querySqlStoreName = DEFAULT_QUERY_SQL_STORE_NAME;
    String mutationSqlStoreName = DEFAULT_MUTATION_SQL_STORE_NAME;
    String deltaSyncSqlStoreName = DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
    String clientDatabasePrefix;

    private enum AuthMode {
        API_KEY("API_KEY"),
        AWS_IAM("AWS_IAM"),
        AMAZON_COGNITO_USER_POOLS("AMAZON_COGNITO_USER_POOLS"),
        OPENID_CONNECT("OPENID_CONNECT");

        private final String name;

        AuthMode(String name) {
            this.name = name;
        }

        /**
         * @return the name of this AuthMode
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the AuthMode enum corresponding to the given AuthMode name.
         *
         * @param authModeName The name of the AuthMode. Ex.: API_KEY
         * @return AuthMode enum representing the given AuthMode name.
         */
        public static AuthMode fromName(String authModeName) {
            for (final AuthMode authMode : AuthMode.values()) {
                if (authModeName.equals(authMode.getName())) {
                    return authMode;
                }
            }
            throw new IllegalArgumentException("Cannot create enum from " + authModeName + " value!");
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private AWSAppSyncClient(AWSAppSyncClient.Builder builder) {
        applicationContext = builder.mContext.getApplicationContext();
        if (builder.mClientDatabasePrefix != null) {
            clientDatabasePrefix = builder.mClientDatabasePrefix;
            querySqlStoreName = clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_QUERY_SQL_STORE_NAME;
            mutationSqlStoreName = clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_MUTATION_SQL_STORE_NAME;
            deltaSyncSqlStoreName = clientDatabasePrefix + DATABASE_NAME_DELIMITER + DEFAULT_DELTA_SYNC_SQL_STORE_NAME;
        }

        //Create the Signer interceptor. The notion of "Signer" is overloaded here as apart
        //from the SigV4 signer, the other signers add request headers and don't sign the request per se
        AppSyncSigV4SignerInterceptor appSyncSigV4SignerInterceptor = null;
        if (builder.mCredentialsProvider != null) {
            appSyncSigV4SignerInterceptor = new AppSyncSigV4SignerInterceptor(builder.mCredentialsProvider, builder.mRegion.getName());
        } else if (builder.mCognitoUserPoolsAuthProvider != null) {
            appSyncSigV4SignerInterceptor = new AppSyncSigV4SignerInterceptor(builder.mCognitoUserPoolsAuthProvider, builder.mRegion.getName());
        } else if (builder.mOidcAuthProvider != null) {
            appSyncSigV4SignerInterceptor = new AppSyncSigV4SignerInterceptor(builder.mOidcAuthProvider);
        } else if (builder.mApiKey != null) {
            appSyncSigV4SignerInterceptor = new AppSyncSigV4SignerInterceptor(builder.mApiKey, builder.mRegion.getName(),  getClientSubscriptionUUID(builder.mApiKey.getAPIKey()));
        } else {
            throw new RuntimeException("Client requires credentials. Please use #apiKey() #credentialsProvider() or #cognitoUserPoolsAuthProvider() to set the credentials.");
        }

        //Create the HTTP client
        OkHttpClient.Builder okHttpClientBuilder;
        if (builder.mOkHttpClient == null) {
            okHttpClientBuilder = new OkHttpClient.Builder();
        } else {
            okHttpClientBuilder = builder.mOkHttpClient.newBuilder();
        }

        //Add the signer and retry handler to the OKHTTP chain
        OkHttpClient okHttpClient = okHttpClientBuilder
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(appSyncSigV4SignerInterceptor)
                .build();

        //Setup up the local store
        if (builder.mNormalizedCacheFactory == null) {
            AppSyncSqlHelper appSyncSqlHelper = AppSyncSqlHelper.create(applicationContext, querySqlStoreName);

            //Create NormalizedCacheFactory
            builder.mNormalizedCacheFactory = new SqlNormalizedCacheFactory(appSyncSqlHelper);
        }

        AppSyncMutationsSqlHelper mutationsSqlHelper = new AppSyncMutationsSqlHelper(builder.mContext, mutationSqlStoreName);
        AppSyncMutationSqlCacheOperations sqlCacheOperations = new AppSyncMutationSqlCacheOperations(mutationsSqlHelper);
        mutationsToRetryAfterConflictResolution = new HashMap<>();

        //Instantiate the optimistic update interceptor
        AppSyncOptimisticUpdateInterceptor optimisticUpdateInterceptor = new AppSyncOptimisticUpdateInterceptor();

        //Instantiate the custom network Invoker
        AppSyncCustomNetworkInvoker networkInvoker =
                new AppSyncCustomNetworkInvoker(HttpUrl.parse(builder.mServerUrl),
                        okHttpClient,
                        new ScalarTypeAdapters(builder.customTypeAdapters),
                        builder.mPersistentMutationsCallback,
                        builder.mS3ObjectManager);

        mAppSyncOfflineMutationManager =  new AppSyncOfflineMutationManager(builder.mContext,
                builder.customTypeAdapters,
                sqlCacheOperations,
                networkInvoker);

        //Create the Apollo Client and setup the interceptor chain.
        ApolloClient.Builder clientBuilder = ApolloClient.builder()
                .serverUrl(builder.mServerUrl)
                .normalizedCache(builder.mNormalizedCacheFactory, builder.mResolver)
                .addApplicationInterceptor(optimisticUpdateInterceptor)
                .addApplicationInterceptor(new AppSyncOfflineMutationInterceptor(
                        mAppSyncOfflineMutationManager,
                        false,
                        builder.mContext,
                        mutationsToRetryAfterConflictResolution,
                        this,
                        builder.mConflictResolver,
                        builder.mMutationQueueExecutionTimeout))
                .addApplicationInterceptor(new AppSyncComplexObjectsInterceptor(builder.mS3ObjectManager))
                .okHttpClient(okHttpClient);

        //Add custom type builders
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

        //Add Subscription manager
        RealSubscriptionManager subscriptionManager = new RealSubscriptionManager(
                builder.mContext.getApplicationContext(),
                builder.mSubscriptionsAutoReconnect);
        clientBuilder.subscriptionManager(subscriptionManager);

        //Build the Apollo Client
        mApolloClient = clientBuilder.build();

        //Add reference to Apollo Client in the Subscription manager.
        subscriptionManager.setApolloClient(mApolloClient);

        mSyncStore = new AppSyncStore(mApolloClient.apolloStore());

        optimisticUpdateInterceptor.setStore(mApolloClient.apolloStore());
        subscriptionManager.setStore(mApolloClient.apolloStore());
        subscriptionManager.setScalarTypeAdapters(new ScalarTypeAdapters(builder.customTypeAdapters));
        mS3ObjectManager = builder.mS3ObjectManager;
    }

    /**
     * Returns the Client Subscription UUID associated with the API_KEY. Creates a new UUID if required.
     *
     * @param apiKey The apiKey
     * @return The client subscription UUID.
     */

    private String getClientSubscriptionUUID(String apiKey) {
        String clientSubscriptionUUID = null;
        final String SHARED_PREFERENCES_FILE_NAME = "com.amazonaws.mobileconnectors.appsync";
        try {
            //Get Shared Preferences
            SharedPreferences appSyncSharedPreferences = applicationContext.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);

            //Create a SHA 256 hash of the API KEY
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            String hash = BinaryUtils.toHex(digest.digest(apiKey.getBytes()));

            clientSubscriptionUUID = appSyncSharedPreferences.getString(hash, null);
            if (clientSubscriptionUUID == null) {
                clientSubscriptionUUID = UUID.randomUUID().toString();
                appSyncSharedPreferences.edit().putString(hash, clientSubscriptionUUID);
            }
        }
        catch (NoSuchAlgorithmException nsae) {
            Log.e(TAG, "Error getting Subscription UUID " + nsae.getLocalizedMessage());
            Log.e(TAG, "Proceeding without Subscription UUID");
        }
        return clientSubscriptionUUID;
    }


    public static class Builder {
        // AWS
        Regions mRegion;
        AWSCredentialsProvider mCredentialsProvider;
        APIKeyAuthProvider mApiKey;
        CognitoUserPoolsAuthProvider mCognitoUserPoolsAuthProvider;
        OidcAuthProvider mOidcAuthProvider;
        NormalizedCacheFactory mNormalizedCacheFactory;
        CacheKeyResolver mResolver;
        ConflictResolverInterface mConflictResolver;
        AWSConfiguration mAwsConfiguration;
        boolean mSubscriptionsAutoReconnect = true;
        long mMutationQueueExecutionTimeout = 5 * 60 * 1000;

        // Apollo
        String mServerUrl;
        final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
        Executor mDispatcher;
        OkHttpClient mOkHttpClient;
        ResponseFetcher mDefaultResponseFetcher = AppSyncResponseFetchers.CACHE_FIRST;
        CacheHeaders mCacheHeaders;
        PersistentMutationsCallback mPersistentMutationsCallback;

        // Android
        // Android application context
        Context mContext;

        // Used for complex objects - upload and download
        S3ObjectManager mS3ObjectManager;

        // This will be the prefix of the client databases (caches) - Query, Mutations and Subscriptions
        String mClientDatabasePrefix;

        // Flag when true uses the prefix passed through the .clientDatabasePrefix(String) builder
        boolean mUseClientDatabasePrefix;

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

        public Builder oidcAuthProvider(OidcAuthProvider oidcAuthProvider) {
            mOidcAuthProvider = oidcAuthProvider;
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

        /**
         * Read the AppSync section of the awsconfiguration.json file and populate the serverUrl and region
         * variables from the ApiUrl and Region keys. The AuthMode specified in the file will be used to
         * validate the Authentication mechanism chosen during the object construction.
         *
         * Example: API_KEY
         *
         * <p>
         * "AppSync": {
         *   "Default": {
         *       "ApiUrl": "https://xxxx.appsync-api.<region>.amazonaws.com/graphql",
         *       "Region": "us-east-1",
         *       "ApiKey": "da2-yyyy",
         *       "AuthMode": "API_KEY"
         *   }
         * }
         * </p>
         *
         * <p>
         *     Usage:
         *          .awsConfiguration(new AWSConfiguration(getApplicationContext())
         *
         * </p>
         * @param awsConfiguration The object representing the configuration
         *                         information from awsconfiguration.json
         *
         * @return the builder object
         */
        public Builder awsConfiguration(AWSConfiguration awsConfiguration) {
            mAwsConfiguration = awsConfiguration;
            return this;
        }

        /**
         * Specify automatic reconnection for Subscriptions in case of errors. Reconnection is enabled by default.
         * @param subscriptionsAutoReconnect true to to enable and false to disable automatic reconnection of subscriptions.
         *
         *
         * @return the builder object
         */
        public Builder subscriptionsAutoReconnect( boolean subscriptionsAutoReconnect) {
            mSubscriptionsAutoReconnect = subscriptionsAutoReconnect;
            return this;
        }

        /**
         * Specify the maximum duration for which a mutation will be allowed to execute before it is evicted from the Mutation queue.
         * Default value is 5 minutes. Note that this limit is for execution time - the mutation can wait in the queue for its turn to
         * be processed independent of this limit.
         * @param mutationQueueExecutionTimeout the max execution time allowed.
         *
         * @return the builder object
         */
        public Builder mutationQueueExecutionTimeout(long mutationQueueExecutionTimeout) {
            mMutationQueueExecutionTimeout = mutationQueueExecutionTimeout;
            return this;
        }

        /**
         * Flag if true will use the ClientDatabasePrefix passed in through the
         * awsconfiguration.json or from the #clientDatabasePrefix builder.
         *
         * @param useClientDatabasePrefix use a prefix for the client databases.
         * @return the builder object.
         */
        public Builder useClientDatabasePrefix(@NonNull boolean useClientDatabasePrefix) {
            mUseClientDatabasePrefix = useClientDatabasePrefix;
            return this;
        }

        /**
         * Specify a name that uniquely identifies the AWSAppSyncClient object.
         *
         * @param clientDatabasePrefix prefix used for the database names.
         * @return the builder object.
         */
        public Builder clientDatabasePrefix(@NonNull String clientDatabasePrefix) {
            mClientDatabasePrefix = clientDatabasePrefix;
            return this;
        }

        /**
         * Extract the parameters from the AWSAppSyncClient.Builder object and create
         * the AWSAppSyncClient object.
         *
         * @return AWSAppSyncClient object built from AWSAppSyncClient.Builder
         */
        public AWSAppSyncClient build() {
            // Read serverUrl, region and AuthMode from awsconfiguration.json if present
            if (mAwsConfiguration != null) {
                try {
                    // Populate the serverUrl and region from awsconfiguration.json
                    JSONObject appSyncJsonObject = mAwsConfiguration.optJsonObject("AppSync");
                    if (appSyncJsonObject == null) {
                        throw new RuntimeException("AppSync configuration is missing from awsconfiguration.json");
                    }

                    mServerUrl = appSyncJsonObject.getString("ApiUrl");
                    mRegion = Regions.fromName(appSyncJsonObject.getString("Region"));

                    if (mUseClientDatabasePrefix) {
                        // Populate the ClientDatabasePrefix from awsconfiguration.json
                        String clientDatabasePrefixFromConfigJson = null;
                        try {
                            clientDatabasePrefixFromConfigJson = appSyncJsonObject.getString("ClientDatabasePrefix");
                        } catch (Exception ex) {
                            Log.e(TAG, "Error is reading the ClientDatabasePrefix from AppSync configuration in awsconfiguration.json.");
                            throw new RuntimeException("ClientDatabasePrefix is not present in AppSync configuration in awsconfiguration.json " +
                                    "however .useClientDatabasePrefix(true) is passed in.");
                        }

                        mClientDatabasePrefix = clientDatabasePrefixFromConfigJson;
                    }

                    Map<Object, AuthMode> authModeObjects = new HashMap<>();
                    authModeObjects.put(mApiKey, AuthMode.API_KEY);
                    authModeObjects.put(mCredentialsProvider, AuthMode.AWS_IAM);
                    authModeObjects.put(mCognitoUserPoolsAuthProvider, AuthMode.AMAZON_COGNITO_USER_POOLS);
                    authModeObjects.put(mOidcAuthProvider, AuthMode.OPENID_CONNECT);
                    authModeObjects.remove(null);

                    // Validate if only one Auth object is passed in to the builder
                    if (authModeObjects.size() > 1) {
                        throw new RuntimeException("More than one AuthMode has been passed in to the builder. " +
                                authModeObjects.values().toString() +
                                ". Please pass in exactly one AuthMode into the builder.");
                    }

                    // Store references to the authMode object passed in to the builder and the
                    // corresponding AuthMode
                    Object selectedAuthModeObject = null;
                    AuthMode selectedAuthMode = null;
                    Iterator<Object> iterator = authModeObjects.keySet().iterator();
                    if (iterator.hasNext()) {
                        selectedAuthModeObject = iterator.next();
                        selectedAuthMode = authModeObjects.get(selectedAuthModeObject);
                    }

                    // Read the AuthMode and validate if the corresponding Auth object is passed in
                    // to the builder
                    final AuthMode authMode = AuthMode.fromName(appSyncJsonObject.getString("AuthMode"));
                    if (selectedAuthModeObject == null && authMode.equals(AuthMode.API_KEY)) {
                        mApiKey = new BasicAPIKeyAuthProvider(appSyncJsonObject.getString("ApiKey"));
                        selectedAuthMode = authMode;
                    }

                    // Validate if the AuthMode match
                    if (!authMode.equals(selectedAuthMode)) {
                        throw new RuntimeException("Found conflicting AuthMode. Should be " +
                                authMode.toString() + " but you selected " + selectedAuthMode.toString());
                    }
                } catch (Exception exception) {
                    throw new RuntimeException("Please check the AppSync configuration in " +
                            "awsconfiguration.json.", exception);
                }
            }

            if (mUseClientDatabasePrefix && (mClientDatabasePrefix == null || StringUtils.isBlank(mClientDatabasePrefix))) {
                throw new RuntimeException("Please pass in a valid ClientDatabasePrefix when useClientDatabasePrefix is true.");
            }

            if (!mUseClientDatabasePrefix && mClientDatabasePrefix != null && !StringUtils.isBlank(mClientDatabasePrefix)) {
                Log.w(TAG, "A ClientDatabasePrefix is passed in however useClientDatabasePrefix is false.");
                // Don't use the client database prefix when mUseClientDatabasePrefix flag is not set or set to false.
                mClientDatabasePrefix = null;
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
     * @return builder object
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
            mutationsToRetryAfterConflictResolution.put(mutation, null);
        }
        return mApolloClient.mutate(mutation);
    }

    protected <D extends Mutation.Data, T, V extends Mutation.Variables> AppSyncMutationCall<T> mutate(@Nonnull Mutation<D, T, V> mutation, @Nonnull D withOptimisticUpdates, boolean isRetry) {
        if (isRetry) {
            mutationsToRetryAfterConflictResolution.put(mutation, null);
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

    public class AWSAppSyncDeltaSyncWatcher implements Cancelable {
        boolean canceled = false;
        long id;

        public AWSAppSyncDeltaSyncWatcher( long id) {
            this.id = id;
        }
        @Override
        public void cancel() {
            if (!canceled) {
                AWSAppSyncDeltaSync.cancel(id);
                canceled = true;
            }
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }
    }

    /**
     * Provides the ability to sync using a baseQuery, deltaQuery, subscription, and a refresh interval
     * @param baseQuery the base query to get the baseline state
     * @param baseQueryCallback callback to handle the baseQuery results
     * @param subscription subscription to get changes on the fly
     * @param subscriptionCallback callback to handle the subscription messages
     * @param deltaQuery the catchup query
     * @param deltaQueryCallback callback to handle the deltaQuery results
     * @param baseRefreshIntervalInSeconds time duration (specified in seconds) when the base query will be re-run to get an updated baseline state.
     * @param <D>
     * @param <T>
     * @param <V>
     * @return a Cancelable object that can be used later to cancel the sync operation by calling the cancel() method
     */
    public <D extends Query.Data, T, V extends Query.Variables> Cancelable sync(
            @Nonnull Query<D, T, V> baseQuery,
            GraphQLCall.Callback<Query.Data> baseQueryCallback,
            Subscription<D,T,V> subscription,
            AppSyncSubscriptionCall.Callback subscriptionCallback,
            Query<D,T,V> deltaQuery,
            GraphQLCall.Callback<Query.Data> deltaQueryCallback,
            long baseRefreshIntervalInSeconds) {
        AWSAppSyncDeltaSync helper = new AWSAppSyncDeltaSync(baseQuery,this, applicationContext);

        helper.setBaseQueryCallback(baseQueryCallback);

        helper.setSubscription(subscription);
        helper.setSubscriptionCallback(subscriptionCallback);

        if (deltaQuery == null || deltaQueryCallback == null) {
            Log.d(TAG, "One of the following is null - Delta Query or Delta Query callback. Will switch to using the base query & callback");
            helper.setDeltaQuery(baseQuery);
            helper.setDeltaQueryCallback(baseQueryCallback);
        } else {
            helper.setDeltaQuery(deltaQuery);
            helper.setDeltaQueryCallback(deltaQueryCallback);
        }

        helper.setBaseRefreshIntervalInSeconds(baseRefreshIntervalInSeconds);

        return new AWSAppSyncDeltaSyncWatcher(helper.execute(false));
    }

    /**
     * Provides the ability to sync using a baseQuery and a refresh interval
     *
     * @param baseQuery the base query to get the baseline state
     * @param baseQueryCallback callback to handle the baseQuery results
     * @param baseRefreshIntervalInSeconds time duration (specified in seconds) when the base query will be re-run to get an updated baseline state.
     * @param <D>
     * @param <T>
     * @param <V>
     * @return a Cancelable object that can be used later to cancel the sync operation by calling the cancel() method
     */
    public <D extends Query.Data, T, V extends Query.Variables> Cancelable sync(
            @Nonnull Query<D, T, V> baseQuery,
            GraphQLCall.Callback<Query.Data> baseQueryCallback,
            long baseRefreshIntervalInSeconds) {

        return this.sync(baseQuery, baseQueryCallback,null, null,null, null, baseRefreshIntervalInSeconds);
    }


    /**
     * Provides the ability to sync using a baseQuery, deltaQuery, and a refresh interval
     * @param baseQuery the base query to get the baseline state
     * @param baseQueryCallback callback to handle the baseQuery results
     * @param deltaQuery the catchup query
     * @param deltaQueryCallback callback to handle the deltaQuery results
     * @param baseRefreshIntervalInSeconds time duration (specified in seconds) when the base query will be re-run to get an updated baseline state.
     * @param <D>
     * @param <T>
     * @param <V>
     * @return a Cancelable object that can be used later to cancel the sync operation by calling the cancel() method
     */
    public <D extends Query.Data, T, V extends Query.Variables> Cancelable sync(
            @Nonnull Query<D, T, V> baseQuery,
            GraphQLCall.Callback<Query.Data> baseQueryCallback,
            Query<D,T,V> deltaQuery,
            GraphQLCall.Callback<Query.Data> deltaQueryCallback,
            long baseRefreshIntervalInSeconds) {
        return this.sync(baseQuery, baseQueryCallback, null, null, deltaQuery, deltaQueryCallback, baseRefreshIntervalInSeconds);
    }


    /**
     * Provides the ability to sync using a baseQuery and Subscription
     * @param baseQuery the base query to get the baseline state
     * @param baseQueryCallback callback to handle the baseQuery results
     * @param subscription subscription to get changes on the fly
     * @param subscriptionCallback callback to handle the subscription messages
     * @param <D>
     * @param <T>
     * @param <V>
     * @return a Cancelable object that can be used later to cancel the sync operation by calling the cancel() method
     */
    public <D extends Query.Data, T, V extends Query.Variables> Cancelable sync(
            @Nonnull Query<D, T, V> baseQuery,
            GraphQLCall.Callback<Query.Data> baseQueryCallback,
            Subscription<D,T,V> subscription,
            AppSyncSubscriptionCall.Callback subscriptionCallback
            ) {
        return this.sync(baseQuery, baseQueryCallback, subscription, subscriptionCallback, null, null, 0 );
    }

    /**
     * Used to check if the mutation queue is empty.
     * @return true if queue is empty, false otherwise.
     */
    public boolean isMutationQueueEmpty() {
        if (mAppSyncOfflineMutationManager != null ) {
            return mAppSyncOfflineMutationManager.mutationQueueEmpty();
        }
        return true;
    }

    /**
     * Clear the mutation queue. A Mutation that is currently in progress will
     * continue to execute until finished.
     *
     * @deprecated Please use
     *     #clearCache(ClearCacheOptions.builder().clearMutations().build()) instead.
     */
    @Deprecated
    public void clearMutationQueue() {
        mAppSyncOfflineMutationManager.clearMutationQueue();
    }

    /**
     * Clear the store created for Delta Sync. This method deletes all the records
     * that stores the lastSyncTime of the sync queries.
     *
     * If there are only on-going delta sync operations, it is recommended to cancel those
     * operations before calling clearCache.
     */
    private void clearDeltaSyncStore() {
        Log.d(TAG, "Clearing the delta sync store.");

        AWSAppSyncDeltaSyncSqlHelper awsAppSyncDeltaSyncSqlHelper =
                new AWSAppSyncDeltaSyncSqlHelper(
                    applicationContext,
                    deltaSyncSqlStoreName);
        new AWSAppSyncDeltaSyncDBOperations(awsAppSyncDeltaSyncSqlHelper)
                .clearDeltaSyncStore();
    }

    /**
     * Clears the different client databases on the local device
     * which stores the following:
     *
     * 1) Query Cache - query responses
     * 2) Mutation Queue - offline persistent mutations
     * 3) Delta Sync Metadata Cache - Subscriptions Metadata
     *      If there are only on-going delta sync operations, it is recommended to cancel those
     *      operations before calling clearCache.
     */
    public void clearCache() throws AWSAppSyncClientException {
        clearCache(ClearCacheOptions.builder()
                .clearQueries()
                .clearMutations()
                .clearSubscriptions()
                .build());
    }

    /**
     * Clears the different client databases on the local device
     * based on the ClearCacheOptions object passed in.
     *
     * 1) ClearCacheOptions#clearQueries - Query Cache - query responses
     * 2) ClearCacheOptions#clearMutations - Mutation Queue - offline persistent mutations
     * 3) ClearCacheOptions#clearSubscriptions - Delta Sync Metadata Cache - Subscriptions Metadata
     *      If there are only on-going delta sync operations, it is recommended to cancel those
     *      operations before calling clearCache.
     */
    public void clearCache(ClearCacheOptions clearCacheOptions) throws AWSAppSyncClientException {
        try {
            if (clearCacheOptions.isQueries()) {
                Log.d(TAG, "Clearing the query cache.");
                mSyncStore.clearAll().execute();
            }

            if (clearCacheOptions.isMutations()) {
                Log.d(TAG, "Clearing the mutations queue.");
                clearMutationQueue();
            }

            if (clearCacheOptions.isSubscriptions()) {
                Log.d(TAG, "Clearing the delta sync subscriptions metadata cache.");
                clearDeltaSyncStore();
            }
        } catch (Exception ex) {
            throw new AWSAppSyncClientException("Error in clearing the cache.", ex);
        }
    }
}
