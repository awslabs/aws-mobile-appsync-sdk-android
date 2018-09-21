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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

public class RealSubscriptionManager implements SubscriptionManager {
    private static final String TAG = RealSubscriptionManager.class.getSimpleName();

    private Context applicationContext;
    private ApolloStore apolloStore;
    private ScalarTypeAdapters scalarTypeAdapters;

    final List<SubscriptionClient> clients;

    final Map<Subscription, SubscriptionObject> subscriptionsById;
    final Map<String, AtomicReference<HashSet<SubscriptionObject>>> subscriptionsByTopic;

    private final Object subscriptionsById_addLock = new Object();
    private final Object subscriptionsByTopic_addLock = new Object();

    public RealSubscriptionManager(@Nonnull final Context applicationContext) {
        this.applicationContext = applicationContext.getApplicationContext();
        subscriptionsById = new ConcurrentHashMap<>();
        subscriptionsByTopic = new ConcurrentHashMap<>();
        clients = new ArrayList<>();
    }

    private SubscriptionObject getSubscriptionObject(Subscription subscription) {
        SubscriptionObject sub = subscriptionsById.get(subscription);

        if (sub != null) {
            return sub;
        }

        synchronized (subscriptionsById_addLock) {
            sub = subscriptionsById.get(subscription);

            if (sub != null) {
                return sub;
            }

            sub = new SubscriptionObject();
            sub.subscription = subscription;
            subscriptionsById.put(subscription, sub);
            return sub;
        }
    }

    /**
     * Used when topic should be added as a key in {@code subscriptionsByTopic} if it does not exist.
     * @param topic the topic to add to or retrieve from {@code subscriptionsByTopic}
     * @return the set of subscriptions that correspond to the topic, does not mutate underlying data
     */
    private Set<SubscriptionObject> getSubscriptionObjects(String topic) {
        AtomicReference<HashSet<SubscriptionObject>> ref = subscriptionsByTopic.get(topic);

        // Return without hitting synchronize block when reading when key is available.
        if (ref != null)
            return ref.get();

        synchronized (subscriptionsByTopic_addLock) {
            ref = subscriptionsByTopic.get(topic);

            if (ref != null)
                return ref.get();

            ref = new AtomicReference<>();
            ref.set(new HashSet<SubscriptionObject>());
            subscriptionsByTopic.put(topic, ref);
            return ref.get();
        }
    }

    /**
     * Adds the {@code subscriptionObject} to the {@code topic} so messages can be directed to the {@code subscriptionObject} when received.
     * @param topic IoT topic i.e. 123123123123/i74uyvnaefymtgyjp15sfzmgxy/onCreatePost/
     * @param subscriptionObject
     */
    private void addSubscriptionObject(String topic, SubscriptionObject subscriptionObject) {
        synchronized (subscriptionsByTopic_addLock) {
            Set<SubscriptionObject> subscriptionObjects = getSubscriptionObjects(topic);
            HashSet<SubscriptionObject> set = new HashSet<>(subscriptionObjects);
            set.add(subscriptionObject);
            Log.d(TAG, "Adding subscription watcher " + subscriptionObject + " to topic " + topic + " total topics: " + set.size());
            subscriptionsByTopic.get(topic).set(set);
        }
    }

    private void removeUnusedTopics(Set<String> activeTopicSet) {
        subscriptionsByTopic.keySet().retainAll(activeTopicSet);
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
    public synchronized  <T> void subscribe(
            @Nonnull Subscription<?, T, ?> subscription,
            @Nonnull final List<String> newTopics,
            @Nonnull SubscriptionResponse response,
            ResponseNormalizer<Map<String, Object>> mapResponseNormalizer) {
        Log.d(TAG, "subscribe called " + newTopics);

        SubscriptionObject subscriptionObject = getSubscriptionObject(subscription);
        subscriptionObject.subscription = subscription;
        subscriptionObject.normalizer = mapResponseNormalizer;
        subscriptionObject.scalarTypeAdapters = this.scalarTypeAdapters;

        for (String topic : newTopics) {
            subscriptionObject.topics.add(topic);
            addSubscriptionObject(topic, subscriptionObject);
        }

        final CountDownLatch allClientsConnectedLatch = new CountDownLatch(response.mqttInfos.size());

        // Create new clients, connections, and subscriptions
        final List<SubscriptionClient> newClients = new ArrayList<>();
        Log.d(TAG, "Attempting to make [" + response.mqttInfos.size() + "] MQTT clients]");
        for (final SubscriptionResponse.MqttInfo info : response.mqttInfos) {

            final SubscriptionClient client = new MqttSubscriptionClient(this.applicationContext, info.wssURL, info.clientId);
            // Silence new clients until swapped with old clients
            client.setTransmitting(false);
            client.connect(new SubscriptionClientCallback() {
                @Override
                public void onConnect() {
                    Set<String> topicSet = subscriptionsByTopic.keySet();
                    Log.d(TAG, String.format("Connection successful. Will subscribe up to %d topics", info.topics.length));
                    for (String topic : info.topics) {
                        if (topicSet.contains(topic)) {
                            Log.d(TAG, String.format("Connecting to topic:[%s]", topic));
                            client.subscribe(topic, 0, mainMessageCallback);
                        }
                    }
                    newClients.add(client);
                    allClientsConnectedLatch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    Map<SubscriptionObject, AppSyncSubscriptionCall.Callback> unsubscribeMap = new HashMap<>();
                    for (String topic : info.topics) {
                        for (SubscriptionObject subObj : getSubscriptionObjects(topic)) {
                            if (e instanceof SubscriptionDisconnectedException) {
                                subObj.onFailure(new ApolloException("Subscription terminated", e));
                                for (Object c : subObj.getListeners()) {
                                    unsubscribeMap.put(subObj, ((AppSyncSubscriptionCall.Callback)c));
                                }
                            } else {
                                subObj.onFailure(new ApolloException("Failed to create client for subscription", e));
                            }
                        }
                    }
                    for (SubscriptionObject subObj: unsubscribeMap.keySet()) {
                        RealSubscriptionManager.this.removeListener(subObj.subscription, unsubscribeMap.get(subObj));
                        RealSubscriptionManager.this.unsubscribe(subObj.subscription);
                    }
                    allClientsConnectedLatch.countDown();
                }
            });
        }


        try {
            allClientsConnectedLatch.await();
            Log.d(TAG, "Made [" + newClients.size() + "] MQTT clients");

            synchronized (clients) {
                // Silence the old clients
                Log.d(TAG, "Muting the old clients [ " + clients.size() + "] in total");
                for (final SubscriptionClient client : clients) {
                    client.setTransmitting(false);
                }

                Log.d(TAG, "Unmuting the new clients [" + newClients.size() + "] in total");
                // Unmute new clients
                for (final SubscriptionClient client : newClients) {
                    client.setTransmitting(true);
                }

                // Close old clients
                Log.d(TAG, "Closing the old clients [" + clients.size() + "] in total");
                for (final SubscriptionClient client : clients) {
                    Log.d(TAG, "Closing client: " + client);
                    client.close();
                }

                clients.clear();
                clients.addAll(newClients);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to wait for all clients to finish connecting.", e);
        }

//        Set<String> activeTopicSet = new HashSet<>();
//        for (final SubscriptionResponse.MqttInfo info : response.mqttInfos) {
//            Collections.addAll(activeTopicSet, info.topics);
//        }
//        removeUnusedTopics(activeTopicSet);
    }

    final private SubscriptionCallback mainMessageCallback = new SubscriptionCallback() {
        @Override
        public void onMessage(String topic, String message) {
            final Set<SubscriptionObject> subscriptionSet = getSubscriptionObjects(topic);

            if (subscriptionSet == null || subscriptionSet.size() == 0) {
                Log.w(TAG, "No listeners for message: " + message + " from topic: " + topic);
            }

            for (SubscriptionObject subObj : subscriptionSet) {
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
        subscriptionsById.remove(subObject);
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
