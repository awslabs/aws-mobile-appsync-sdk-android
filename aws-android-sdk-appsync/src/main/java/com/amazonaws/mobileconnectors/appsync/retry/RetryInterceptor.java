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
    private static final int JITTER = 100;

    @Override
    public Response intercept(Chain chain) throws IOException {
        int retryCount = 0;
        Response response;
        do {

            //Send the request on to the next link in the chain of processors
            response = chain.proceed(chain.request());

            //Exit function if response was successful
            if (response.isSuccessful()) {
                Log.i(TAG, "Returning network response: success");
                return response;
            }


            //Check if server has sent a Retry-After header attribute in the response.
            //If so, respect that!
            final String retryAfterHeaderValue = response.header("Retry-After");
            if (retryAfterHeaderValue != null) {
                try {
                    int waitMillis = Integer.parseInt(retryAfterHeaderValue) * 1000;
                    Log.d(TAG, "Sleeping for " + waitMillis + " ms as per server's Retry-After header value");
                    sleep(waitMillis);
                    continue;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse Retry-After header: " + retryAfterHeaderValue);
                    Log.w(TAG, "Will proceed with exponential backoff strategy");
                }
            }

            //Compute backoff and sleep if error is retriable
            if ((response.code() >= 500 && response.code() < 600)
                    || response.code() == 429 ) {
                final int backOff = (int) Math.min(Math.pow(2, retryCount) * 100 + (Math.random() * JITTER), MAX_RETRY_WAIT_MILLIS) ;
                Log.d(TAG, "Sleeping for " + backOff + " ms as per exponential backoff sequence");
                sleep(backOff);
                continue;
            }

            //Not a 5XX or 429 error. Don't retry
            Log.d(TAG, "Encountered non-retriable error. Returning response");
            return response;

        } while (retryCount++ < MAX_RETRY_COUNT);

        Log.i(TAG, "Returning network response, retries exhausted");
        return response;
    }

    private void sleep(int waitMillis) {
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Log.e(TAG, "Retry **wait** interrupted.");
        }
    }
}
