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

package com.amazonaws.mobileconnectors.appsync.subscription;

import android.content.Context;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.subscription.mqtt.MqttSubscriptionClient;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class RealSubscriptionManager implements SubscriptionManager {
    private static final String TAG = RealSubscriptionManager.class.getSimpleName();

    private Context applicationContext;
    private ApolloStore apolloStore;
    private ScalarTypeAdapters scalarTypeAdapters;

    final List<SubscriptionClient> clients;

    final Map<Subscription, SubscriptionObject> subscriptionsById;
    final Map<String, Set<SubscriptionObject>> subscriptionsByTopic;

    public RealSubscriptionManager(@Nonnull final Context applicationContext) {
        this.applicationContext = applicationContext.getApplicationContext();
        subscriptionsById = new HashMap<>();
        subscriptionsByTopic = new HashMap<>();
        clients = new ArrayList<>();
    }

    private SubscriptionObject getSubscriptionObject(Subscription subscription) {
        SubscriptionObject sub = subscriptionsById.get(subscription);
        if (sub == null) {
            sub = new SubscriptionObject();
            sub.subscription = subscription;
            subscriptionsById.put(subscription, sub);
        }
        return sub;
    }

    private Set<SubscriptionObject> getSubscriptionObjects(String topic) {
        synchronized (subscriptionsByTopic) {
            Set<SubscriptionObject> set = subscriptionsByTopic.get(topic);
            if (set == null) {
                set = new HashSet<>();
                subscriptionsByTopic.put(topic, set);
            }
            return set;
        }
    }

    private void removeUnusedTopics(String[] activeTopics) {
        synchronized (subscriptionsByTopic) {
            Map<String, Set<SubscriptionObject>> newSubscriptionsByTopic = new HashMap<>();
            for (String topic : activeTopics) {
                newSubscriptionsByTopic.put(topic, getSubscriptionObjects(topic));
            }

            subscriptionsByTopic.clear();
            subscriptionsByTopic.putAll(newSubscriptionsByTopic);
        }
    }

    public void addListener(Subscription subscription, AppSyncSubscriptionCall.Callback callback) {
        SubscriptionObject subObject = getSubscriptionObject(subscription);
        Log.d(TAG, "Adding " + callback.toString() + " listener to subObject: " + subscription + " got: " + subObject.subscription);
        subObject.addListener(callback);
    }

    public void removeListener(Subscription subscription, AppSyncSubscriptionCall.Callback callback) {
        SubscriptionObject subObject = getSubscriptionObject(subscription);
        subObject.listeners.remove(callback);
        if (subObject.listeners.size() == 0) {
            for (Object topic : subObject.topics) {
                getSubscriptionObjects((String) topic).remove(subObject);
            }
        }
    }

    @Override
    public <T> void subscribe(
            @Nonnull Subscription<?, T, ?> subscription,
            @Nonnull final List<String> subbedTopics,
            @Nonnull SubscriptionResponse response,
            ResponseNormalizer<Map<String, Object>> mapResponseNormalizer) {
        Log.d(TAG, "sub manager subscribe called");

        SubscriptionObject sub = getSubscriptionObject(subscription);
        sub.subscription = subscription;
        sub.normalizer = mapResponseNormalizer;
        sub.scalarTypeAdapters = this.scalarTypeAdapters;

        for (String topic : subbedTopics) {
            sub.topics.add(topic);
            getSubscriptionObjects(topic).add(sub);
            Log.d(TAG, "New topics for this sub" + sub + topic);
        }

        Log.d(TAG, "Number of my topics: " + subscriptionsByTopic.keySet().size());
        for (Object topic : subscriptionsByTopic.keySet()) {
            Log.d(TAG, "Listening to topic: " + topic);
        }

        // Create new clients, connections, and subscriptions
        List<SubscriptionClient> newClients = new ArrayList<>();
        Log.d(TAG, "Preparing to make Mqtt client count: " + response.mqttInfos.size());
        for (final SubscriptionResponse.MqttInfo info : response.mqttInfos) {
            final SubscriptionClient client = new MqttSubscriptionClient(this.applicationContext, info.wssURL, info.clientId);
            client.setTransmitting(false);
            Log.d(TAG, "Notclosure Topic length: " + info.topics.length);
            Log.d(TAG, "Notclosure My topic length: " + subscriptionsByTopic.keySet().size());
            client.connect(new SubscriptionClientCallback() {
                @Override
                public void onConnect() {
                    Log.d(TAG, "Topic length: " + info.topics.length);
                    Log.d(TAG, "My topic length: " + subscriptionsByTopic.keySet().size());
                    for (String topic : info.topics) {
                        if (subscriptionsByTopic.keySet().contains(topic)) {
                            client.subscribe(topic, 0, mainMessageCallback);
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    for (String topic : info.topics) {
                        for (SubscriptionObject subObj : getSubscriptionObjects(topic)) {
                            subObj.onFailure(new ApolloException("Failed to create client for subscription", e));
                        }
                    }
                }
            });
            newClients.add(client);
        }

        synchronized (clients) {
            // Silence the old clients
            Log.d(TAG, "Old client count: " + clients);
            for (final SubscriptionClient client : clients) {
                client.setTransmitting(false);
            }

            // Unmute new clients
            for (final SubscriptionClient client : newClients) {
                client.setTransmitting(true);
            }

            // Close old clients
            for (final SubscriptionClient client : clients) {
                Log.d(TAG, "Closing client: " + client);
                client.close();
            }

            clients.clear();
            clients.addAll(newClients);
        }
//        for (final SubscriptionResponse.MqttInfo info : response.mqttInfos) {
//            removeUnusedTopics(info.topics);
//        }
    }

    final private SubscriptionCallback mainMessageCallback = new SubscriptionCallback() {
        @Override
        public void onMessage(String topic, String message) {
            Log.d(TAG, "subs per this topic: " + getSubscriptionObjects(topic).size());
            for (SubscriptionObject subObj : getSubscriptionObjects(topic)) {
                Log.d(TAG, "Send " + subObj.subscription + " msg " + message + " for topic" + topic);
                subObj.onMessage(message);
            }
        }

        @Override
        public void onError(String topic, Exception e) {
            for (SubscriptionObject subObj : getSubscriptionObjects(topic)) {
                subObj.onFailure(new ApolloException("Failed to subscribe to topic", e));
            }
        }
    };

    @Override
    public void unsubscribe(@Nonnull Subscription<?, ?, ?> subscription) {
        SubscriptionObject subObject = getSubscriptionObject(subscription);
        for (Object topic : subObject.getTopics()) {
            getSubscriptionObjects((String) topic).remove(subObject);
        }
        subObject.getTopics().clear();
    }

    @Override
    public void setStore(ApolloStore apolloStore) {
        this.apolloStore = apolloStore;
    }

    @Override
    public void setScalarTypeAdapters(ScalarTypeAdapters scalarTypeAdapters) {
        this.scalarTypeAdapters = scalarTypeAdapters;
    }
}
