/**
 * Copyright 2021 Amazon.com,,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.retry;

import android.support.annotation.NonNull;

import com.amazonaws.mobileconnectors.appsync.util.Await;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertTrue;

/**
 * Tests for retry interceptor.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class RetryInterceptorTest {
    public static final int TEST_TIMEOUT = 10;
    private MockWebServer mockWebServer;
    private OkHttpClient okHttpClient;

    @Before
    public void beforeEachTest() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8888);

        okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(new RetryInterceptor())
            .build();
    }

    @After
    public void afterEachTest() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * Verify that everything works when the first attempt succeeds.
     * @throws IOException Not expected
     * @throws InterruptedException Not expected
     */
    @Test
    public void successfulRequestWithoutFailuresTest() throws Throwable {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"result\":\"all good\""));

        final Request request = new Request.Builder()
            .url("http://localhost:8888")
            .method("POST", RequestBody.create("{}", MediaType.get("application/json")))
            .build();

        Response response = Await.result(new Await.ResultErrorEmitter<Response, Throwable>() {
            @Override
            public void emitTo(@NonNull Consumer<Response> onResult, @NonNull Consumer<Throwable> onError) {
                okHttpClient.newCall(request).enqueue(new OkHttpCallback(onResult, onError));
            }
        });
        assertTrue(response.body().string().contains("all good"));
    }

    /**
     * Verify that retries happen successfully without leaving the previous response open.
     * This test was created as a result of a Github issue
     * https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/305.
     * @throws IOException Not expected
     * @throws InterruptedException Not expected
     */
    @Test
    public void successfulRequestWithFailuresTest() throws Throwable {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"some exception\""));
        mockWebServer.enqueue(new MockResponse().setResponseCode(501).setBody("{\"error\":\"another exception\"").setHeader("Retry-After", "1"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"some exception\""));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"result\":\"all good\""));

        final Request request = new Request.Builder()
            .url("http://localhost:8888")
            .method("POST", RequestBody.create("{}", MediaType.get("application/json")))
            .build();

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"result\":\"all good\""));

        Response response = Await.result(new Await.ResultErrorEmitter<Response, Throwable>() {
            @Override
            public void emitTo(@NonNull Consumer<Response> onResult, @NonNull Consumer<Throwable> onError) {
                okHttpClient.newCall(request).enqueue(new OkHttpCallback(onResult, onError));
            }
        });
        assertTrue(response.body().string().contains("all good"));
    }

    /**
     * Wrapper class that takes the two Consumer callbacks from the Await.result function
     * and uses them to emit result or error.
     */
    static final class OkHttpCallback implements Callback {
        private final AtomicBoolean success = new AtomicBoolean(false);
        private final Consumer<Response> onResponse;
        private final Consumer<Throwable> onError;

        public OkHttpCallback(Consumer<Response> onResponse, Consumer<Throwable> onError) {
            this.onResponse = onResponse;
            this.onError = onError;
        }

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            onError.accept(e);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            onResponse.accept(response);
        }
    }
}
