package com.amazonaws.postsapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsCallback;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsError;
import com.amazonaws.mobileconnectors.appsync.PersistentMutationsResponse;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;


public class ClientFactory {
    private static volatile AWSAppSyncClient client;

    public synchronized static AWSAppSyncClient getInstance(Context context) {
        if (client == null) {
            client = AWSAppSyncClient.builder()
                    .context(context)
                    .apiKey(new BasicAPIKeyAuthProvider(Constants.APPSYNC_API_KEY)) // For use with IAM/Cognito authorization
                    .region(Constants.APPSYNC_REGION)
                    .serverUrl(Constants.APPSYNC_API_URL)
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
}