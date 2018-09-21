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

package com.amazonaws.mobileconnectors.appsync.subscription.mqtt;

import android.content.Context;
import android.util.Log;

import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionCallback;
import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionClient;
import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionClientCallback;
import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionObject;
import com.amazonaws.mobileconnectors.appsync.subscription.SubscriptionDisconnectedException;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MqttSubscriptionClient implements SubscriptionClient {
    private static final String TAG = MqttSubscriptionClient.class.getSimpleName();

    MqttAndroidClient mMqttAndroidClient;
    /**
     * key: topic
     */
    public final Map<String, Set<SubscriptionObject>> subsMap;
    MessageListener msgListener;
    ClientConnectionListener clientConnectionListener;

    public MqttSubscriptionClient(Context applicationContext, String wssURL, String clientId) {
        mMqttAndroidClient = new MqttAndroidClient(applicationContext, wssURL, clientId, new MemoryPersistence());
        subsMap = new HashMap<>();
        msgListener = new MessageListener();
        msgListener.client = this;
        msgListener.setTransmitting(false);
    }

    @Override
    public void connect(final SubscriptionClientCallback callback) {
        try {
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setKeepAliveInterval(60);
            clientConnectionListener = new ClientConnectionListener(callback);
            mMqttAndroidClient.setCallback(clientConnectionListener);
            Log.d(TAG, "Calling MQTT Connect with actual endpoint");
            mMqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (callback != null) {
                        callback.onConnect();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (callback != null) {
                        callback.onError(new Exception(exception));
                    }
                }
            });
        } catch (MqttException e) {
            Log.e("TAG", "Failed to connect mqtt client for subscriptions", e);
            callback.onError(e);
        }
    }

    @Override
    public void subscribe(final String topic, int qos, final SubscriptionCallback callback) {
        try {
            Log.d(TAG, this + " Attempt to subscribe to topic " + topic);
            if (msgListener != null) {
                msgListener.setCallback(callback);
            }
            mMqttAndroidClient.subscribe(topic, qos, msgListener);
        } catch (MqttException e) {
            callback.onError(topic, e);
        }
    }

    @Override
    public void unsubscribe(String topic) {
        try {
            mMqttAndroidClient.unsubscribe(topic, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setTransmitting(final boolean isTransmitting) {
        if (msgListener != null) {
            msgListener.setTransmitting(isTransmitting);
        }
        if (this.clientConnectionListener != null) {
            this.clientConnectionListener.isTransmitting = isTransmitting;
        }
    }

    @Override
    public void close() {
        final String clientRepresentation = mMqttAndroidClient.toString();
        try {
            //Disconnect the connection with quiese timeout set to 0, which means disconnect immediately.
            //Issue the close connection on the callback - this ensures that the connection is properly closed out before resources are freed/reclaimed by the close method.
            mMqttAndroidClient.disconnect(0,null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            mMqttAndroidClient.close();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Closing " + clientRepresentation + " mqtt client", e);
        }
    }

    class MessageListener implements IMqttMessageListener {
        public MqttSubscriptionClient client;
        SubscriptionCallback callback;
        private boolean isTransmitting;

        public void setCallback(SubscriptionCallback callback) {
            this.callback = callback;
        }

        public void setTransmitting(boolean truth) {
            Log.d(TAG, "Set transmit " + truth + " " + client);
            this.isTransmitting = truth;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(TAG, client + " transmit: " + isTransmitting + " mqttL: " + this + "subL: " + callback + " Topic: " + topic + " Msg: " + message.toString());
            if (isTransmitting) {
                callback.onMessage(topic, message.toString());
            }
        }
    }

    class ClientConnectionListener implements MqttCallback {
        private boolean isTransmitting;
        final SubscriptionClientCallback callback;

        public ClientConnectionListener(final SubscriptionClientCallback callback) {
            this.callback = callback;
            isTransmitting = true;
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "connection lost isTransmitting: " + isTransmitting);
            if (isTransmitting) {
                callback.onError(new SubscriptionDisconnectedException("Client disconnected", cause));
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(TAG, "message arrived");
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "delivery complete");
        }
    }
}
