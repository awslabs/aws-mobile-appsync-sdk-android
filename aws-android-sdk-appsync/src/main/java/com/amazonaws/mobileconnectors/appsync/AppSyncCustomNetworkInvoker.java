/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;


import android.os.Message;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.appsync.utils.StringUtils;
import com.amazonaws.util.VersionInfoUtils;
import com.apollographql.apollo.api.S3InputObjectInterface;
import com.apollographql.apollo.api.S3ObjectManager;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import org.json.JSONArray;
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

    private static final String TAG = AppSyncCustomNetworkInvoker.class.getSimpleName();

    final HttpUrl serverUrl;
    final okhttp3.Call.Factory httpCallFactory;
    final ScalarTypeAdapters scalarTypeAdapters;
    final PersistentMutationsCallback persistentMutationsCallback;
    final  S3ObjectManager s3ObjectManager;
    Executor dispatcher;
    volatile Call httpCall;
    volatile boolean disposed;
    AppSyncOfflineMutationInterceptor.QueueUpdateHandler queueHandler;
    PersistentOfflineMutationManager persistentOfflineMutationManager;

    public AppSyncCustomNetworkInvoker(@Nonnull HttpUrl serverUrl,
                                       @Nonnull Call.Factory httpCallFactory,
                                       @Nonnull ScalarTypeAdapters scalarTypeAdapters,
                                       @Nullable PersistentMutationsCallback mutationsCallback,
                                       @Nullable S3ObjectManager s3ObjectManager) {
        this.serverUrl = checkNotNull(serverUrl, "serverUrl == null");
        this.httpCallFactory = checkNotNull(httpCallFactory, "httpCallFactory == null");
        this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
        this.persistentMutationsCallback = mutationsCallback;
        this.dispatcher = defaultDispatcher();
        this.s3ObjectManager = s3ObjectManager;
    }

    void updateQueueHandler(AppSyncOfflineMutationInterceptor.QueueUpdateHandler queueHandler) {
        this.queueHandler = queueHandler;
    }

    void setPersistentOfflineMutationManager( PersistentOfflineMutationManager persistentOfflineMutationManager) {
        this.persistentOfflineMutationManager = persistentOfflineMutationManager;
    }


    private Executor defaultDispatcher() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(@Nonnull Runnable runnable) {
                return new Thread(runnable, "AppSync Persistent Mutations Dispatcher");
            }
        });
    }

    public void executeRequest(final PersistentOfflineMutationObject persistentOfflineMutationObject) {
        dispatcher.execute(new Runnable() {
                               @Override
                               public void run() {

                                   //Upload S3 Object if present in the mutation. The S3 Object is uploaded first before the mutation is executed.
                                   if (!persistentOfflineMutationObject.bucket.equals("")) {
                                       //Fail if S3 Object Manager has not been provided
                                       if (s3ObjectManager == null) {
                                           //Invoke callback
                                           if (persistentMutationsCallback != null ) {
                                               persistentMutationsCallback.onFailure(new PersistentMutationsError(persistentOfflineMutationObject.responseClassName,
                                                       persistentOfflineMutationObject.recordIdentifier, new ApolloNetworkException("S3 upload failed.", new IllegalArgumentException("S3ObjectManager not provided."))));
                                           }
                                           //Remove mutation from queue and mark state as completed.
                                           setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);

                                           //Trigger next mutation
                                           queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                                           return;
                                       }

                                       try {
                                           s3ObjectManager.upload(new S3InputObjectInterface() {
                                               @Override
                                               public String localUri() {
                                                   return persistentOfflineMutationObject.localURI;
                                               }

                                               @Override
                                               public String mimeType() {
                                                   return persistentOfflineMutationObject.mimeType;
                                               }

                                               @Override
                                               public String bucket() {
                                                   return persistentOfflineMutationObject.bucket;
                                               }

                                               @Override
                                               public String key() {
                                                   return persistentOfflineMutationObject.key;
                                               }

                                               @Override
                                               public String region() {
                                                   return persistentOfflineMutationObject.region;
                                               }
                                           });
                                       }
                                       catch (AmazonClientException e) {

                                           if ( e.getCause() instanceof IOException ) {
                                               //IO Exception occured indicating that there was a network issue.
                                               //Set mutationInProgress status to false and return without removing the mutation from the queue.
                                               queueHandler.setMutationInProgressStatusToFalse();
                                               return;
                                           }

                                           //Not a network error.
                                           if (persistentMutationsCallback != null ) {
                                               persistentMutationsCallback.onFailure(new PersistentMutationsError(persistentOfflineMutationObject.responseClassName,
                                                       persistentOfflineMutationObject.recordIdentifier, new ApolloNetworkException("S3 upload failed.", e)));
                                           }

                                           //Remove mutation from queue and mark state as completed.
                                           setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);
                                           //Trigger next mutation
                                           queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                                           return;
                                       }
                                       catch (Exception e) {
                                           //Invoke callback
                                           if (persistentMutationsCallback != null ) {
                                               persistentMutationsCallback.onFailure(new PersistentMutationsError(persistentOfflineMutationObject.responseClassName,
                                                       persistentOfflineMutationObject.recordIdentifier, new ApolloNetworkException("S3 upload failed.", e)));
                                           }
                                           //Remove mutation from queue and mark state as completed.
                                           setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);
                                           //Trigger next mutation
                                           queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                                           return;
                                       }
                                   }

                                   httpCall = httpCall(persistentOfflineMutationObject);
                                   //Execute the mutation
                                   httpCall.enqueue(new Callback() {
                                       @Override
                                       public void onFailure(@Nonnull Call call, @Nonnull IOException e) {

                                           Log.e(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Failed to execute http call for [" +
                                                   persistentOfflineMutationObject.recordIdentifier +"]. Exception is [" + e + "]");

                                           //If this request has been canceled.
                                           if (disposed) {
                                               setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);
                                               queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                                               return;
                                           }

                                           //An I/O Exception indicates that the request didn't hit the server, likely due to a network issue.
                                           //Mark this current execution as complete and wait for the next network event to resume queue processing
                                           queueHandler.setMutationInProgressStatusToFalse();
                                           return;
                                       }

                                       @Override
                                       public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
                                           if (disposed) {
                                               setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);
                                               queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                                               return;
                                           }

                                           if (response.isSuccessful()) {
                                               String responseString = response.body().string();

                                               try {
                                                   JSONObject jsonObject = new JSONObject(responseString);

                                                   //If conflict was present, route it through for conflict handling
                                                   if ( ConflictResolutionHandler.conflictPresent(responseString) ) {
                                                       JSONArray errors = jsonObject.optJSONArray("errors");
                                                       MutationInterceptorMessage interceptorMessage = new MutationInterceptorMessage();
                                                       interceptorMessage.requestIdentifier = persistentOfflineMutationObject.recordIdentifier;
                                                       interceptorMessage.clientState = persistentOfflineMutationObject.clientState;
                                                       interceptorMessage.requestClassName = persistentOfflineMutationObject.responseClassName;
                                                       interceptorMessage.serverState = new JSONObject(errors.getJSONObject(0).getString("data")).toString();

                                                       Message message = new Message();
                                                       message.obj = interceptorMessage;
                                                       message.what = MessageNumberUtil.RETRY_EXEC;
                                                       queueHandler.sendMessage(message);
                                                       return;
                                                   }

                                                   if (persistentMutationsCallback != null) {
                                                       persistentMutationsCallback.onResponse(new PersistentMutationsResponse(
                                                               jsonObject.optJSONObject("data"),
                                                               jsonObject.optJSONArray("errors"),
                                                               persistentOfflineMutationObject.responseClassName,
                                                               persistentOfflineMutationObject.recordIdentifier));
                                                   }
                                                   setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);
                                                   queueHandler.sendEmptyMessage(MessageNumberUtil.SUCCESSFUL_EXEC);
                                               } catch (JSONException e) {
                                                   e.printStackTrace();
                                                   Log.d(TAG, "Thread:[" + Thread.currentThread().getId() + "]: JSON Parse error" + e.toString());
                                                   if (persistentMutationsCallback != null) {
                                                       persistentMutationsCallback.onFailure(
                                                               new PersistentMutationsError(persistentOfflineMutationObject.responseClassName,
                                                                       persistentOfflineMutationObject.recordIdentifier,
                                                                       new ApolloParseException("Failed to parse http response", e)));
                                                   }
                                                   setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);
                                                   queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                                               }
                                           } else {
                                               //Invoke onFailure on the callback.
                                               if (persistentMutationsCallback != null) {
                                                   persistentMutationsCallback.onFailure(
                                                           new PersistentMutationsError(persistentOfflineMutationObject.responseClassName,
                                                                   persistentOfflineMutationObject.recordIdentifier,
                                                                   new ApolloNetworkException("Failed to execute http call with error code and message: " + response.code() + response.message())));
                                               }
                                               setMutationExecutionAsCompletedAndRemoveFromQueue(persistentOfflineMutationObject);
                                               queueHandler.sendEmptyMessage(MessageNumberUtil.FAIL_EXEC);
                                           }
                                       }
                                   });
                               }
                           }
        );
    }

    private void setMutationExecutionAsCompletedAndRemoveFromQueue
            (PersistentOfflineMutationObject p) {

        //This mutation is completed. So remove it from the queue.
        persistentOfflineMutationManager.removePersistentMutationObject(p.recordIdentifier);

        if (persistentOfflineMutationManager.getTimedoutMutations().contains(p)) {
            //Mutation has been tagged as timed out. Remove this from the timedOutList.
            //The queueHandler will manage getting the queue unblocked for this case.
            persistentOfflineMutationManager.removeTimedoutMutation(p);
        }
        else {
            //Mutation not tagged as timed out. Signal queueHandler that this mutation is done.
            queueHandler.setMutationInProgressStatusToFalse();
        }
    }

    private Call httpCall(PersistentOfflineMutationObject mutationObject) {
        RequestBody requestBody = RequestBody.create(MEDIA_TYPE, mutationObject.requestString);
        String userAgent = StringUtils.toHumanReadableAscii(VersionInfoUtils.getUserAgent());

        Request.Builder requestBuilder = new Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .addHeader(HEADER_USER_AGENT, userAgent + " OfflineMutation")
                .header(HEADER_ACCEPT_TYPE, ACCEPT_TYPE)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE);
        return httpCallFactory.newCall(requestBuilder.build());
    }

}
