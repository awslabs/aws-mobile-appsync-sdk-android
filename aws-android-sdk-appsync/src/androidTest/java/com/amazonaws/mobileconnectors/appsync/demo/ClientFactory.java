package com.amazonaws.mobileconnectors.appsync.demo;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsCallback;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsError;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsResponse;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;

public class ClientFactory {
    private static volatile AWSAppSyncClient client;

    private static volatile AWSConfiguration awsConfiguration;

    public synchronized static AWSAppSyncClient getInstance(Context context) {
        if (client == null) {
            // awsConfiguration = new AWSConfiguration(context);

            client = AWSAppSyncClient.builder()
                    .context(context)
                    .apiKey(new BasicAPIKeyAuthProvider(Constants.APPSYNC_API_KEY))
                    .serverUrl(Constants.APPSYNC_API_URL)
                    .region(Constants.APPSYNC_REGION)
                    .persistentMutationsCallback(new PersistentMutationsCallback() {
                        @Override
                        public void onResponse(PersistentMutationsResponse response) {
                            Log.d("NOTERROR", response.getMutationClassName());
                        }

                        @Override
                        public void onFailure(PersistentMutationsError error) {
                            Log.e("TAG", error.getMutationClassName());
                            Log.e("TAG", "Error", error.getException());
                        }
                    })
                    .build();
        }
        return client;
    }

    private static final AWSCredentialsProvider getCredentialsProvider(final Context context) {
        final CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                awsConfiguration
        );
        return credentialsProvider;
    }
}