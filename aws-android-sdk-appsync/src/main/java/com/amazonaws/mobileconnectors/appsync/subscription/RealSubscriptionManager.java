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
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.RealAppSyncSubscriptionCall;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public class RealSubscriptionManager implements SubscriptionManager {
    private static final String TAG = RealSubscriptionManager.class.getSimpleName();

    private Context applicationContext;
    private ApolloStore apolloStore;
    private ScalarTypeAdapters scalarTypeAdapters;
    private ApolloClient mApolloClient = null;

    final List<SubscriptionClient> clients;

    final Map<Subscription, SubscriptionObject> subscriptionsById;
    final Map<String, HashSet<SubscriptionObject>> subscriptionsByTopic;
    final Map<String, MqttSubscriptionClient> topicConnectionMap;

    private final Object subscriptionsByIdLock = new Object();
    private final Object subscriptionsByTopicLock = new Object();

    public RealSubscriptionManager(@Nonnull final Context applicationContext) {
        this.applicationContext = applicationContext.getApplicationContext();
        subscriptionsById = new ConcurrentHashMap<>();
        subscriptionsByTopic = new ConcurrentHashMap<>();
        topicConnectionMap = new ConcurrentHashMap<>();
        clients = new ArrayList<>();
    }

    public void setApolloClient(ApolloClient apolloClient) {
        this.mApolloClient = apolloClient;
    }

    //Return associated SubscriptionObject from subscriptionsById map using the Subscription.
    private SubscriptionObject getSubscriptionObjectFromIdMap(Subscription subscription) {
        synchronized (subscriptionsByIdLock) {
            return subscriptionsById.get(subscription);
        }
    }

    //Return associated SubscriptionObject from subscriptionsById map using the Subscription.
    //Create and Add a subscription object if required
    private SubscriptionObject createAndAddSubscriptionObjectToIdMap(Subscription subscription) {
        synchronized (subscriptionsByIdLock) {
            SubscriptionObject sub = subscriptionsById.get(subscription);
            if (sub == null) {
                sub = new SubscriptionObject();
                sub.subscription = subscription;
                subscriptionsById.put(subscription, sub);
            }
            return sub;
        }
    }

    //Remove Subscription Object from subscriptionsById map
    private void removeSubscriptionObjectFromIdMap(SubscriptionObject subscriptionObject) {
        synchronized (subscriptionsByIdLock) {
            subscriptionObject.getTopics().clear();
            subscriptionsById.remove(subscriptionObject);
        }
    }

    /**
     * Used when topic should be added as a key in {@code subscriptionsByTopic} if it does not exist.
     * @param topic the topic to add to or retrieve from {@code subscriptionsByTopic}
     * @return the set of subscriptions that correspond to the topic, does not mutate underlying data
     */
    private Set<SubscriptionObject> createSubscriptionsObjectSetinTopicMap(String topic) {

        synchronized (subscriptionsByTopicLock) {
            HashSet<SubscriptionObject> set  = subscriptionsByTopic.get(topic);
            if (set == null) {
                set = new HashSet<SubscriptionObject>();
                subscriptionsByTopic.put(topic, set);
            }
            return set;
        }
    }

    private Set<SubscriptionObject> getSubscriptionObjectSetFromTopicMap(String topic) {
       synchronized (subscriptionsByTopicLock) {
            return subscriptionsByTopic.get(topic);
        }
    }


    /**
     * Adds the {@code subscriptionObject} to the {@code topic} so messages can be directed to the {@code subscriptionObject} when received.
     * @param topic IoT topic i.e. 123123123123/i74uyvnaefymtgyjp15sfzmgxy/onCreatePost/
     * @param subscriptionObject
     */
    private void addSubscriptionObjectToTopic(String topic, SubscriptionObject subscriptionObject) {
        synchronized (subscriptionsByTopicLock) {
            Set<SubscriptionObject> subscriptionObjectsSet = getSubscriptionObjectSetFromTopicMap(topic);
            if (subscriptionObjectsSet == null ) {
                subscriptionObjectsSet = createSubscriptionsObjectSetinTopicMap(topic);
            }
            subscriptionObjectsSet.add(subscriptionObject);
            Log.d(TAG, "Subscription Infrastructure: Adding subscription object " + subscriptionObject + " to topic " + topic + ". Total subscription objects: " + subscriptionObjectsSet.size());
        }
    }

    private void removeUnusedTopics(Set<String> activeTopicSet) {
        subscriptionsByTopic.keySet().retainAll(activeTopicSet);
    }

    public void addListener(Subscription subscription, AppSyncSubscriptionCall.Callback listener) {
        synchronized (subscriptionsByIdLock) {
            SubscriptionObject subscriptionObject = getSubscriptionObjectFromIdMap(subscription);
            if ( subscriptionObject == null ) {
                subscriptionObject = createAndAddSubscriptionObjectToIdMap(subscription);
            }

            Log.v(TAG, "Subscription Infrastructure: Adding listener [" + listener.toString() + "] to SubscriptionObject: " + subscription + " got: " + subscriptionObject.subscription);
            subscriptionObject.addListener(listener);
        }
    }

    public void removeListener(Subscription subscription, AppSyncSubscriptionCall.Callback listener) {
        synchronized (subscriptionsByIdLock) {
            SubscriptionObject subscriptionObject = getSubscriptionObjectFromIdMap(subscription);
            if (subscriptionObject == null ) {
                return;
            }

            subscriptionObject.listeners.remove(listener);
            if (subscriptionObject.listeners.size() == 0) {
                for (Object topic : subscriptionObject.topics) {
                    Set<SubscriptionObject> subscriptionObjectsSet = getSubscriptionObjectSetFromTopicMap(topic.toString());
                    if (subscriptionObjectsSet != null ) {
                        subscriptionObjectsSet.remove(subscriptionObject);
                    }
                }
            }
        }
    }

    @Override
    public synchronized  <T> void subscribe(
            @Nonnull Subscription<?, T, ?> subscription,
            @Nonnull final List<String> newTopics,
            @Nonnull SubscriptionResponse response,
            ResponseNormalizer<Map<String, Object>> mapResponseNormalizer) {
        Log.v(TAG, "Subscription Infrastructure: subscribe called for " + subscription);

        //Look up from or register subscription in the subscriptionsById map.
        SubscriptionObject subscriptionObject = getSubscriptionObjectFromIdMap(subscription);
        if ( subscriptionObject == null ) {
            subscriptionObject = createAndAddSubscriptionObjectToIdMap(subscription);
        }
        subscriptionObject.subscription = subscription;
        subscriptionObject.normalizer = mapResponseNormalizer;
        subscriptionObject.scalarTypeAdapters = this.scalarTypeAdapters;

        //Add the new topics to this Subscription Object
        //and add the subscriptions to the topic map.
        for (String topic : newTopics) {
            subscriptionObject.topics.add(topic);
            addSubscriptionObjectToTopic(topic, subscriptionObject);
        }

        final CountDownLatch allClientsConnectedLatch = new CountDownLatch(response.mqttInfos.size());

        // Create new clients, connections, and subscriptions
        final List<SubscriptionClient> newClients = new ArrayList<>();
        Log.v(TAG, "Subscription Infrastructure: Attempting to make [" + response.mqttInfos.size() + "] MQTT clients]");
        for (final SubscriptionResponse.MqttInfo info : response.mqttInfos) {

            final MqttSubscriptionClient mqttClient = new MqttSubscriptionClient(this.applicationContext, info.wssURL, info.clientId);
            // Silence new clients until swapped with old clients
            mqttClient.setTransmitting(false);
            Log.v(TAG, "Subscription Infrastructure: Connecting with Client ID[" + info.clientId +"]");
            mqttClient.connect(new SubscriptionClientCallback() {
                @Override
                public void onConnect() {
                    reportSuccessfulConnection();
                    Set<String> topicSet = subscriptionsByTopic.keySet();
                    Log.v(TAG, String.format("Subscription Infrastructure: Connection successful for clientID [" + info.clientId + "]. Will subscribe up to %d topics", info.topics.length));
                    for (String topic : info.topics) {
                        if (topicSet.contains(topic)) {
                            Log.v(TAG, String.format("Subscription Infrastructure: Subscribing to MQTT topic:[%s]", topic));
                            mqttClient.subscribe(topic, 1, mainMessageCallback);
                            topicConnectionMap.put(topic, mqttClient);
                        }
                    }
                    newClients.add(mqttClient);
                    allClientsConnectedLatch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    Log.v(TAG, "Subscription Infrastructure: onError called");
                    if ( e instanceof SubscriptionDisconnectedException) {
                        Log.v(TAG, "Subscription Infrastructure: Disconnect received. Unexpected - Initiating reconnect sequence.");
                        reportConnectionError();
                        initiateReconnectSequence();
                    }
                    allClientsConnectedLatch.countDown();
                }
            });
        }


        try {
            allClientsConnectedLatch.await();
            Log.v(TAG, "Subscription Infrastructure: Made [" + newClients.size() + "] MQTT clients");

            Log.v(TAG, "Subscription Infrastructure: Unmuting the new clients [" + newClients.size() + "] in total");
            // Unmute new clients.
            for (final SubscriptionClient client : newClients) {
                client.setTransmitting(true);
            }

            // Silence the old clients
            Log.v(TAG, "Subscription Infrastructure: Muting the old clients [ " + clients.size() + "] in total");
            for (final SubscriptionClient client : clients) {
                client.setTransmitting(false);
            }

            // Close old clients
            Log.v(TAG, "Subscription Infrastructure: Closing the old clients [" + clients.size() + "] in total");
            for (final SubscriptionClient client : clients) {
                Log.v(TAG, "Subscription Infrastructure: Closing client: " + client);
                client.close();
            }

            //Add the new clients
            clients.clear();
            clients.addAll(newClients);
        } catch (InterruptedException e) {
            throw new RuntimeException("Subscription Infrastructure: Failed to wait for all clients to finish connecting.", e);
        }
    }

    final private SubscriptionCallback mainMessageCallback = new SubscriptionCallback() {
        @Override
        public void onMessage(String topic, String message) {
            Log.v(TAG, "Subscription Infrastructure: Received message on topic [" + topic +"]. Message is \n" + message);
            final Set<SubscriptionObject> subscriptionObjectsSet = getSubscriptionObjectSetFromTopicMap(topic);
            if (subscriptionObjectsSet == null ) {
                Log.w(TAG, "Subscription Infrastructure: No subscription objects found for topic [" + topic+"]");
            }
            else {
                for (SubscriptionObject subscriptionObject : subscriptionObjectsSet) {
                    Log.v(TAG, "Subscription Infrastructure: Propagating message received on topic " + topic + " to " + subscriptionObject.subscription);
                    subscriptionObject.onMessage(message);
                }
            }
        }

        @Override
        public void onError(String topic, Exception e) {
            final Set<SubscriptionObject> subscriptionObjects = getSubscriptionObjectSetFromTopicMap(topic);
            if (subscriptionObjects == null || subscriptionObjects.size() == 0) {
                Log.w(TAG, "Subscription Infrastructure: No subscription objects found for topic [" + topic+"]");
            }
            else {
                for (SubscriptionObject subscriptionObject : subscriptionObjects) {
                    subscriptionObject.onFailure(new ApolloException("Subscription Infrastructure: onError called for Subscription [" + subscriptionObject +"]", e));
                }
            }
        }
    };

    @Override
    public synchronized void unsubscribe(@Nonnull Subscription<?, ?, ?> subscription) {
        //Get matching subscription object from the subscriptionsById map
        SubscriptionObject subscriptionObject = getSubscriptionObjectFromIdMap(subscription);
        if (subscriptionObject == null ) {
            return;
        }

        // Mark subscriptionObject as cancelled.
        subscriptionObject.setCancelled();

        //Remove subscriptionObject from all associated topics
        for (Object topic : subscriptionObject.getTopics()) {
            synchronized (subscriptionsByTopicLock) {
                Set<SubscriptionObject> subscriptionObjectsSet = getSubscriptionObjectSetFromTopicMap(topic.toString());
                if (subscriptionObjectsSet != null ) {
                    subscriptionObjectsSet.remove(subscriptionObject);
                }
            }
        }
        //Remove Subscription Object from subscriptionsById map
        removeSubscriptionObjectFromIdMap(subscriptionObject);


        //Sweep through all the topics. Unsubscribe all topics that have 0 subscriptions.
        synchronized (subscriptionsByTopicLock) {
            for (String topic: subscriptionsByTopic.keySet() ) {
                Set<SubscriptionObject> subscriptionObjectsSet = getSubscriptionObjectSetFromTopicMap(topic);
                if ( subscriptionObjectsSet != null && subscriptionObjectsSet.size() > 0 ) {
                    Log.v(TAG, "Subscription Infrastructure: SubscriptionObjects still exist for topic [" + topic + "]. Will not unsubscribe at the MQTT level");
                    continue;
                }

                Log.v(TAG, "Subscription Infrastructure: Number of SubscriptionObjects for topic [" + topic + "] is 0. Unsubscribing at the MQTT Level...");
                //Get connection from the topic to Connection map.
                MqttSubscriptionClient client = topicConnectionMap.get(topic);
                if (client == null ) {
                    continue;
                }
                //Call unsubscribe at the MQTT level
                client.unsubscribe(topic);
                subscriptionsByTopic.remove(topic);

                //Check if the client has any topics left. If not, close it.
                if (client.getTopics() == null || client.getTopics().size() == 0) {
                    Log.v(TAG, "Subscription Infrastructure: MQTT Client has no active topics. Disconnecting...");
                    client.close();
                }
            }
        }

    }

    @Override
    public void setStore(ApolloStore apolloStore) {
        this.apolloStore = apolloStore;
    }

    @Override
    public void setScalarTypeAdapters(ScalarTypeAdapters scalarTypeAdapters) {
        this.scalarTypeAdapters = scalarTypeAdapters;
    }

    //Reconnection management
    Thread reconnectThread = null;
    Object reconnectionLock = new Object();
    boolean reconnectionInProgress = false;
    private CountDownLatch reconnectCountdownLatch = null;

    private static final int BASE_RETRY_WAIT_MILLIS = 100;
    private static final int MAX_RETRY_WAIT_MILLIS = 300 * 1000; //Five Minutes
    private static final int JITTER = 100;

    void initiateReconnectSequence() {
        
        synchronized(reconnectionLock) {
            if (reconnectionInProgress) {
                return;
            }
            reconnectionInProgress = true;
            reconnectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int retryCount = 0;
                    long waitMillis = 0;
                    Log.v(TAG, "Subscription Infrastructure: Initiating reconnect sequence");
                    while (reconnectionInProgress) {
                        //Calculate backoff value
                        if (waitMillis < MAX_RETRY_WAIT_MILLIS ) {
                            waitMillis = Math.min((long) (Math.pow(2, retryCount) * BASE_RETRY_WAIT_MILLIS + (Math.random() * JITTER)), MAX_RETRY_WAIT_MILLIS);
                        }
                        try {
                            Log.v(TAG, "Subscription Infrastructure: Sleeping for [" + (waitMillis/1000)+ "] seconds");
                            Thread.sleep(waitMillis);
                        }
                        catch (InterruptedException ie) {
                            Log.v(TAG, "SubscriptionInfrastructure: Thread.sleep was interrupted in the exponential backoff for reconnects");
                        }
                        //Grab any subscription object to retry.
                        SubscriptionObject subscriptionToRetry = null;
                        for (SubscriptionObject subscriptionObject : subscriptionsById.values()) {
                            if (!subscriptionObject.isCancelled()) {
                                subscriptionToRetry = subscriptionObject;
                            }
                        }

                        //Initiate Retry if required
                        if (subscriptionToRetry != null) {
                            Log.v(TAG, "Subscription Infrastructure: Attempting to reconnect");
                            reconnectCountdownLatch = new CountDownLatch(1);
                            mApolloClient.subscribe(subscriptionToRetry.subscription).execute((AppSyncSubscriptionCall.Callback) subscriptionToRetry.getListeners().iterator().next());

                            try {
                                reconnectCountdownLatch.await(1, TimeUnit.MINUTES);
                            } catch (InterruptedException ioe) {
                                Log.v(TAG, "Subscription Infrastructure: Wait interrupted.");
                            }
                        }
                        else {
                            reconnectionInProgress = false;
                        }
                        retryCount++;
                    }
                }
            });
            reconnectThread.start();
        }
    }

    void reportSuccessfulConnection() {
        synchronized (reconnectionLock) {
            if (!reconnectionInProgress) {
                return;
            }
            Log.v(TAG, "Subscription Infrastructure: Successful connection reported!");
            reconnectionInProgress = false;

            if (reconnectCountdownLatch != null ) {
                Log.v(TAG, "Subscription Infrastructure: Counting down the latch");
                reconnectCountdownLatch.countDown();
            }
            if (reconnectThread != null && Thread.State.TERMINATED != reconnectThread.getState()) {
                Log.v(TAG, "Subscription Infrastructure: Interrupting the thread.");
                reconnectThread.interrupt();
            }
        }
    }

    public void reportConnectionError() {
        synchronized (reconnectionLock) {
            if (!reconnectionInProgress) {
                return;
            }
            Log.v(TAG, "Subscription Infrastructure: Connection Error reported!");

            if ( reconnectCountdownLatch != null ) {
                Log.v(TAG, "Subscription Infrastructure: Counting down the latch");
                reconnectCountdownLatch.countDown();
            }
        }
    }

    public void reportNetworkUp() {
        synchronized (reconnectionLock) {
            if (!reconnectionInProgress) {
                return;
            }
            if (reconnectThread != null && Thread.State.TERMINATED != reconnectThread.getState()) {
                Log.v(TAG, "Subscription Infrastructure: Network is up. Interrupting the thread for immediate reconnect.");
                reconnectThread.interrupt();
            }
        }
    }
}
