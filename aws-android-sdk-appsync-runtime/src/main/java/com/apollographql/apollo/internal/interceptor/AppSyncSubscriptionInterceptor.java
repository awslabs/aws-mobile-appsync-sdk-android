/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.internal.interceptor;

import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionResponse;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.json.ApolloJsonReader;
import com.apollographql.apollo.internal.json.BufferedSourceJsonReader;
import com.apollographql.apollo.internal.json.ResponseJsonStreamReader;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

public class AppSyncSubscriptionInterceptor implements ApolloInterceptor {

    private final SubscriptionManager mSubscriptionManager;
    private final ResponseNormalizer<Map<String, Object>> mapResponseNormalizer;

    public AppSyncSubscriptionInterceptor(
            SubscriptionManager subscriptionManager,
            ResponseNormalizer<Map<String, Object>> mapResponseNormalizer) {
        this.mSubscriptionManager = subscriptionManager;
        this.mapResponseNormalizer = mapResponseNormalizer;
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull ApolloInterceptorChain chain, @Nonnull final Executor dispatcher, @Nonnull final CallBack callBack) {
        final boolean isSubscription = request.operation instanceof Subscription;

        if (!isSubscription) {
            chain.proceedAsync(request, dispatcher, callBack);
            return;
        }

        chain.proceedAsync(request, dispatcher, new CallBack() {
            @Override
            public void onResponse(@Nonnull final InterceptorResponse response) {
                dispatcher.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Declared here to allow error message to have scope
                        Map<String, Object> responseMap = null;
                        try {
                            ResponseJsonStreamReader responseStreamReader = ApolloJsonReader.responseJsonStreamReader(new BufferedSourceJsonReader(response.httpResponse.get().body().source()));
                            responseMap = responseStreamReader.toMap();

                            /* The response is of the form
                               extensions
                                    subscription
                                        mqttConnections
                                            [ url, topics [], clientID ]
                                        newSubscriptions
                                            individualSub
                                                topic, expireTime
                               data
                                    individualSubscriptions
                             */

                            // Get Subscription information by looking through the response
                            Map<String, LinkedHashMap> extensions = (Map) responseMap.get("extensions");
                            Map<String, Object> subscriptions = (Map) extensions.get("subscription");
                            List<Map<String, Object>> mqttConnections = (List) subscriptions.get("mqttConnections");

                            // Collect all the topics listed under newSubscriptions in the newTopics var.
                            List<String> newTopics = new ArrayList<>();
                            Collection<Map> newSubscriptions = ((Map) subscriptions.get("newSubscriptions")).values();
                            for (Map subscriptionInstance : newSubscriptions) {
                                if (subscriptionInstance.containsKey("topic")) {
                                    newTopics.add((String) subscriptionInstance.get("topic"));
                                }
                            }

                            SubscriptionResponse subscriptionResponse = new SubscriptionResponse();
                            for (Map<String, Object> mqttConnection : mqttConnections) {
                                final String clientId = (String) mqttConnection.get("client");
                                final String wssURI = (String) mqttConnection.get("url");
                                final String[] preExistingTopics = ((List<String>) mqttConnection.get("topics")).toArray(new String[0]);

                                subscriptionResponse.add(new SubscriptionResponse.MqttInfo(clientId, wssURI, preExistingTopics));
                            }
                            AppSyncSubscriptionInterceptor.this.mSubscriptionManager.subscribe((Subscription) request.operation, newTopics, subscriptionResponse, mapResponseNormalizer);

                            Response parsedResponse = parseSubscription(request.operation, response);
                            callBack.onResponse(new InterceptorResponse(response.httpResponse.get(), parsedResponse, null));
                        } catch (Exception e) {
                            try {
                                callBack.onFailure(new ApolloException("Failed to parse subscription response: " + responseMap, e));
                            } catch (Exception e1) {
                                callBack.onFailure(new ApolloException("Failed to parse subscription response, failed to get body string", e));
                            }
                        } finally {
                            callBack.onCompleted();
                        }
                    }
                });
            }

            @Override
            public void onFetch(FetchSourceType sourceType) {
                callBack.onFetch(sourceType);
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {

                callBack.onFailure(e);
            }

            @Override
            public void onCompleted() {
            /* call onCompleted in onResponse in case of error */
            }
        });
    }

    private <W> Response<W> parseSubscription(Operation<?, W, ?> operation, InterceptorResponse response) {
        return Response.<W>builder(operation).data(null).build();
    }

    @Override
    public void dispose() {

    }
}
