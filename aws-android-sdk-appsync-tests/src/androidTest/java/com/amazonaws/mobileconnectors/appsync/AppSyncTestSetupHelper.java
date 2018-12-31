/**
 * Copyright 2018-2018 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 * <p>
 * http://aws.amazon.com/asl/
 * <p>
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.apollographql.apollo.api.S3ObjectManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

class AppSyncTestSetupHelper {
    private final static String TAG = AppSyncTestSetupHelper.class.getSimpleName();
    private static String bucketName = null;
    private static String s3Region = null;

    static final String getBucketName() {
        return bucketName;
    }

    static final String getS3Region() {
        return s3Region;
    }

    static AWSAppSyncClient createAppSyncClientWithIAM() {
        return createAppSyncClientWithIAM(true);
    }

    static AWSAppSyncClient createAppSyncClientWithIAM(boolean subscriptionsAutoReconnect) {
        InputStream configInputStream = null;
        try {

            configInputStream = InstrumentationRegistry.getContext().getResources().getAssets().open("appsync_test_credentials.json");

            final Scanner in = new Scanner(configInputStream);
            final StringBuilder sb = new StringBuilder();
            while (in.hasNextLine()) {
                sb.append(in.nextLine());
            }
            JSONObject config = new JSONObject(sb.toString());
            String endPoint = config.getString("AppSyncEndpoint");
            String appSyncRegion = config.getString("AppSyncRegion");
            s3Region = config.getString("AppSyncS3Region");
            bucketName = config.getString("AppSyncS3Bucket");
            Log.d(TAG, "Connecting to " + endPoint + ", region: " + appSyncRegion + ", using IAM");
            String cognitoIdentityPoolID = config.getString("CognitoIdentityPoolId");
            String cognitoRegion = config.getString("CognitoIdentityPoolRegion");

            if (endPoint == null || appSyncRegion == null || cognitoIdentityPoolID == null || cognitoRegion == null) {
                Log.e(TAG, "Unable to read AppSyncEndpoint, AppSyncRegion, CognitoIdentityPoolId, CognitoIdentityPoolRegion from config file ");
                return null;
            }

            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(InstrumentationRegistry.getContext(), cognitoIdentityPoolID, Regions.fromName(cognitoRegion));

            AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider);
            s3Client.setRegion(Region.getRegion(s3Region));
            S3ObjectManager s3ObjectManager = new S3ObjectManagerImplementation((s3Client));

            return AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getContext())
                    .credentialsProvider(credentialsProvider)
                    .serverUrl(endPoint)
                    .region(Regions.fromName(appSyncRegion))
                    .s3ObjectManager(s3ObjectManager)
                    .subscriptionsAutoReconnect(subscriptionsAutoReconnect)
                    .persistentMutationsCallback(new PersistentMutationsCallback() {
                        @Override
                        public void onResponse(PersistentMutationsResponse response) {
                            Log.d(TAG, response.getMutationClassName());
                        }

                        @Override
                        public void onFailure(PersistentMutationsError error) {
                            Log.e(TAG, error.getMutationClassName());
                            Log.e(TAG, "Error", error.getException());
                        }
                    })
                    .build();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        } catch (JSONException je) {
            Log.e(TAG, je.getLocalizedMessage());
            je.printStackTrace();
        } finally {
            if (configInputStream != null) {
                try {
                    configInputStream.close();
                } catch (IOException ce) {
                    Log.e(TAG, ce.getLocalizedMessage());
                    ce.printStackTrace();
                }
            }
        }
        return null;
    }

    static AWSAppSyncClient createAppSyncClientWithAPIKEY() {
        return createAppSyncClientWithAPIKEY(true);
    }

    static AWSAppSyncClient createAppSyncClientWithAPIKEY( boolean subscriptionsAutoReconnect) {
        InputStream configInputStream = null;
        try {

            configInputStream = InstrumentationRegistry.getContext().getResources().getAssets().open("appsync_test_credentials.json");

            final Scanner in = new Scanner(configInputStream);
            final StringBuilder sb = new StringBuilder();
            while (in.hasNextLine()) {
                sb.append(in.nextLine());
            }
            JSONObject config = new JSONObject(sb.toString());
            String endPoint = config.getString("AppSyncEndpointAPIKey");
            String appSyncRegion = config.getString("AppSyncRegionAPIKey");
            String apiKey = config.getString("AppSyncAPIKey");
            s3Region = config.getString("AppSyncS3Region");
            bucketName = config.getString("AppSyncS3Bucket");
            Log.d(TAG, "Connecting to " + endPoint + ", region: " + appSyncRegion + ", using APIKEY");
            String cognitoIdentityPoolID = config.getString("CognitoIdentityPoolId");
            String cognitoRegion = config.getString("CognitoIdentityPoolRegion");


            if (endPoint == null || apiKey == null) {
                Log.e(TAG, "Unable to read AppSyncEndpointAPIKey, AppSyncRegionAPIKey or AppSyncAPIKey from config file ");
                return null;
            }


            APIKeyAuthProvider provider = new BasicAPIKeyAuthProvider(apiKey);

            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(InstrumentationRegistry.getContext(), cognitoIdentityPoolID, Regions.fromName(cognitoRegion));
            AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider);
            s3Client.setRegion(Region.getRegion(s3Region));
            S3ObjectManager s3ObjectManager = new S3ObjectManagerImplementation((s3Client));

            return AWSAppSyncClient.builder()
                    .context(InstrumentationRegistry.getContext())
                    .apiKey(provider)
                    .serverUrl(endPoint)
                    .region(Regions.fromName(appSyncRegion))
                    .s3ObjectManager(s3ObjectManager)
                    .subscriptionsAutoReconnect(subscriptionsAutoReconnect)
                    .persistentMutationsCallback(new PersistentMutationsCallback() {
                        @Override
                        public void onResponse(PersistentMutationsResponse response) {
                            Log.d(TAG, response.getMutationClassName());
                        }

                        @Override
                        public void onFailure(PersistentMutationsError error) {
                            Log.e(TAG, error.getMutationClassName());
                            Log.e(TAG, "Error", error.getException());
                        }
                    })
                    .build();

        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        } catch (JSONException je) {
            Log.e(TAG, je.getLocalizedMessage());
            je.printStackTrace();
        } finally {
            if (configInputStream != null) {
                try {
                    configInputStream.close();
                } catch (IOException ce) {
                    Log.e(TAG, ce.getLocalizedMessage());
                    ce.printStackTrace();
                }
            }
        }
        return null;
    }

    static String createDataFile(String fileName, String data) {
        File f = new File(InstrumentationRegistry.getContext().getCacheDir() + fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(data.getBytes());
        }
        catch (IOException e) {
            return null;
        }
        finally {
            try {
                fos.close();
            }
            catch (IOException e) {

            }
        }
        return f.getAbsolutePath();
    }
}
