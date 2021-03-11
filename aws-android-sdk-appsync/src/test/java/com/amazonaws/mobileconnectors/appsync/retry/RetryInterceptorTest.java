/**
 * Copyright 2021 Amazon.com,,
 * Inc. or its affiliates. All Rights Reserved.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync.retry;

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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
    public void successfulRequestWithoutFailuresTest() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"result\":\"all good\""));

        Request request = new Request.Builder()
            .url("http://localhost:8888")
            .method("POST", RequestBody.create("{}", MediaType.get("application/json")))
            .build();

        final AtomicBoolean successful = new AtomicBoolean(false);
        final CountDownLatch requestLatch = new CountDownLatch(1);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                requestLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                if (response.code() < 300) {
                    assertTrue(response.body().string().contains("all good"));
                    successful.set(true);
                }
                requestLatch.countDown();
            }
        });
        requestLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertTrue(successful.get());
    }

    /**
     * Verify that retries happen successfully without leaving the previous response open.
     * This test was created as a result of a Github issue
     * https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/305.
     * @throws IOException Not expected
     * @throws InterruptedException Not expected
     */
    @Test
    public void successfulRequestWithFailuresTest() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"some exception\""));
        mockWebServer.enqueue(new MockResponse().setResponseCode(501).setBody("{\"error\":\"another exception\"").setHeader("Retry-After", "1"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"some exception\""));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"result\":\"all good\""));

        Request request = new Request.Builder()
            .url("http://localhost:8888")
            .method("POST", RequestBody.create("{}", MediaType.get("application/json")))
            .build();

        final AtomicBoolean successful = new AtomicBoolean(false);
        final CountDownLatch requestLatch = new CountDownLatch(1);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                requestLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                if (response.code() < 300) {
                    assertTrue(response.body().string().contains("all good"));
                    successful.set(true);
                }
                requestLatch.countDown();
            }
        });
        requestLatch.await(10, TimeUnit.SECONDS);
        assertTrue(successful.get());
    }
}
