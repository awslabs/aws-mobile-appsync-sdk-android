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
    private static final int MAX_RETRY_COUNT = 12;
    private static final int BASE_RETRY_WAIT_MILLIS = 100;
    private static final int MAX_RETRY_WAIT_MILLIS = 300 * 1000; //Five Minutes
    private static final int JITTER = 100;

    @Override
    public Response intercept(Chain chain) throws IOException {
        // The first call does not count towards retry count
        int retryCount = -1;
        Response response;
        int waitMillis = 0;
        Log.d(TAG, "Retry Interceptor called");
        do {
            sleep(waitMillis);
            //Send the request on to the next link in the chain of processors
            try {
                response = chain.proceed(chain.request());
            }
            catch (IOException ioe) {
                //Log Exception and propagate it back
                Log.w(TAG,"Encountered IO Exception making HTTP call [" + ioe + "]");
                throw ioe;
            }
            //Exit function if response was successful
            if (response.isSuccessful()) {
                Log.i(TAG, "Returning network response: success");
                return response;
            }

            retryCount++;

            //Check if server has sent a Retry-After header attribute in the response.
            //If so, respect that!
            final String retryAfterHeaderValue = response.header("Retry-After");
            if (retryAfterHeaderValue != null) {
                try {
                    waitMillis = Integer.parseInt(retryAfterHeaderValue) * 1000;
                    continue;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse Retry-After header: " + retryAfterHeaderValue);
                    Log.w(TAG, "Will proceed with exponential backoff strategy");
                }
            }

            //Compute backoff and sleep if error is retriable
            if ((response.code() >= 500 && response.code() < 600)
                    || response.code() == 429 ) {
                waitMillis = calculateBackoff(retryCount);
                continue;
            }

            //Not a 5XX or 429 error. Don't retry
            Log.d(TAG, "Encountered non-retriable error. Returning response");
            return response;

        } while (waitMillis < MAX_RETRY_WAIT_MILLIS);

        Log.i(TAG, "Returning network response, retries exhausted");
        return response;
    }

    private void sleep(int waitMillis) {
        try {
            if (waitMillis > 0 ) {
                Log.d(TAG, "Will sleep for " + waitMillis + " ms as per backoff sequence");
                Thread.sleep(waitMillis);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Retry **wait** interrupted.");
        }
    }

    public static int calculateBackoff(int retryCount) {
        if ( retryCount >= MAX_RETRY_COUNT ) {
            return MAX_RETRY_WAIT_MILLIS;
        }
        return (int) Math.min((Math.pow(2, retryCount) * BASE_RETRY_WAIT_MILLIS + (Math.random() * JITTER)), MAX_RETRY_WAIT_MILLIS);
    }
}

