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

import android.util.Log;

import com.amazonaws.util.VersionInfoUtils;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * AppSyncCustomNetworkInvoker.
 */

public class AppSyncCustomNetworkInvoker {
    private static final String HEADER_ACCEPT_TYPE = "Accept";
    private static final String HEADER_CONTENT_TYPE = "CONTENT_TYPE";
    private static final String ACCEPT_TYPE = "application/json";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    final HttpUrl serverUrl;
    final okhttp3.Call.Factory httpCallFactory;
    final ScalarTypeAdapters scalarTypeAdapters;
    final PersistentMutationsCallback persistentMutationsCallback;
    Executor dispatcher;
    volatile Call httpCall;
    volatile boolean disposed;

    public AppSyncCustomNetworkInvoker(@Nonnull HttpUrl serverUrl,
                                       @Nonnull Call.Factory httpCallFactory,
                                       @Nonnull ScalarTypeAdapters scalarTypeAdapters,
                                       @Nullable PersistentMutationsCallback mutationsCallback) {
        this.serverUrl = checkNotNull(serverUrl, "serverUrl == null");
        this.httpCallFactory = checkNotNull(httpCallFactory, "httpCallFactory == null");
        this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
        this.persistentMutationsCallback = mutationsCallback;
        this.dispatcher = defaultDispatcher();
    }

    private Executor defaultDispatcher() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override public Thread newThread(@Nonnull Runnable runnable) {
                return new Thread(runnable, "AppSync Persistent Mutations Dispatcher");
            }
        });
    }

    public void executeRequest(final PersistentOfflineMutationObject mutationObject) {
        dispatcher.execute(new Runnable() {
                               @Override
                               public void run() {
                                   try {
                                       httpCall = httpCall(mutationObject);
                                   } catch (IOException e) {
                                       Log.e("AppSync", "Failed to prepare http call for operation: " + mutationObject.responseClassName);
                                       persistentMutationsCallback.onFailure(
                                               new PersistentMutationsError(mutationObject.responseClassName,
                                                       mutationObject.recordIdentifier,
                                                       new ApolloNetworkException("Failed to prepare http call", e)));
                                       return;
                                   }

                                   httpCall.enqueue(new Callback() {
                                       @Override public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                                           if (disposed) return;
                                           Log.e("AppSync", "Failed to execute http call for operation : " + mutationObject.responseClassName);
                                           if (persistentMutationsCallback != null) {
                                               persistentMutationsCallback.onFailure(
                                                       new PersistentMutationsError(mutationObject.responseClassName,
                                                               mutationObject.recordIdentifier,
                                                               new ApolloNetworkException("Failed to execute http call", e)));
                                           }
                                       }

                                       @Override public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
                                           if (disposed) return;
                                           if (response.isSuccessful()) {
                                               String responseString = response.body().string();

                                               try {
                                                   JSONObject jsonObject = new JSONObject(responseString);
                                                   JSONObject data = jsonObject.getJSONObject("data");
                                                   JSONObject errors = jsonObject.getJSONObject("errors");
                                                   if(persistentMutationsCallback != null) {
                                                       persistentMutationsCallback.onResponse(new PersistentMutationsResponse(
                                                               data,
                                                               errors,
                                                               mutationObject.responseClassName,
                                                               mutationObject.recordIdentifier));
                                                   }
                                               } catch (JSONException e) {
                                                   Log.d("AppSync", "JSON Parse error");
                                                   if(persistentMutationsCallback != null) {
                                                       persistentMutationsCallback.onFailure(
                                                               new PersistentMutationsError(mutationObject.responseClassName,
                                                                       mutationObject.recordIdentifier,
                                                                       new ApolloParseException("Failed to parse http response", e)));
                                                   }
                                               }


                                           } else {
                                               if (persistentMutationsCallback != null) {
                                                   persistentMutationsCallback.onFailure(
                                                           new PersistentMutationsError(mutationObject.responseClassName,
                                                                   mutationObject.recordIdentifier,
                                                                   new ApolloNetworkException("Failed to execute http call with error code and message: " + response.code() + response.message())));
                                               }
                                           }

                                       }
                                   });
                               }
                           }
        );
    }

    private Call httpCall(PersistentOfflineMutationObject mutationObject) throws IOException {
        RequestBody requestBody = httpRequestBody(mutationObject);
        Request.Builder requestBuilder = new Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .addHeader(HEADER_USER_AGENT, VersionInfoUtils.getUserAgent() + " OfflineMutation")
                .header(HEADER_ACCEPT_TYPE, ACCEPT_TYPE)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE);

        return httpCallFactory.newCall(requestBuilder.build());
    }

    private RequestBody httpRequestBody(PersistentOfflineMutationObject mutationObject) throws IOException {
        return RequestBody.create(MEDIA_TYPE, mutationObject.requestString);
    }

}
