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
    private static final int BASE_RETRY_WAIT_MILLIS = 100;
    private static final int MAX_RETRY_WAIT_MILLIS = 300 * 1000; //Five Minutes
    private static final int JITTER = 100;

    @Override
    public Response intercept(Chain chain) throws IOException {
        int retryCount = -1;
        Response response;
        int waitMillis = 0;
        do {
            sleep(waitMillis);
            //Send the request on to the next link in the chain of processors
            response = chain.proceed(chain.request());

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
                waitMillis = (int) (Math.pow(2, retryCount) * BASE_RETRY_WAIT_MILLIS + (Math.random() * JITTER));
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
}

