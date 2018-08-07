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

package com.amazonaws.mobileconnectors.appsync.retry;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Retry logic for 5XX and 429 HTTP error codes.
 * This class uses exponential backoff strategy with full jitter.
 * When "Retry-After" header is specified, the header value will override the backoff strategy.
 */
public class RetryInterceptor implements Interceptor {
    private static final String TAG = RetryInterceptor.class.getSimpleName();
    // The first call does not count towards retry count
    private static final int MAX_RETRY_COUNT = 3;
    private static final int MAX_RETRY_WAIT_MILLIS = 5000;

    @Override
    public Response intercept(Chain chain) throws IOException {
        int retryCount = 0;
        Response response;
        do {
            response = chain.proceed(chain.request());
            if (response.isSuccessful()) {
                Log.i(TAG, "Returning network response: success");
                return response;
            }

            boolean retryAfterSet = false;
            int waitMillis = 0;

            final String retryAfterHeaderValue = response.header("Retry-After");
            if (retryAfterHeaderValue != null) {
                try {
                    waitMillis = Integer.parseInt(retryAfterHeaderValue) * 1000;
                    retryAfterSet = true;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse Retry-After header: " + retryAfterHeaderValue);
                }
            }

            if ((response.code() >= 500 && response.code() < 600)
                    || response.code() == 429) {

                if (!retryAfterSet) {
                    final double calculateBackoffMillis = Math.min(Math.pow(2, retryCount) * 100, MAX_RETRY_WAIT_MILLIS);
                    final double randomizedBackoffMillis = Math.random() * calculateBackoffMillis;
                    waitMillis = (int) randomizedBackoffMillis;
                }

                try {
                    Log.i(TAG, "Waiting " + waitMillis + " milliseconds to retry based on service response.");
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Exponential backoff or Retry-Ater header based retry **wait** failed.");
                }
            } else {
                Log.d(TAG, "Returning network response, fail unknown error code default return");
                return response;
            }
        } while (retryCount++ < MAX_RETRY_COUNT);

        Log.i(TAG, "Returning network response, default return, retries exhausted");
        return response;
    }
}
