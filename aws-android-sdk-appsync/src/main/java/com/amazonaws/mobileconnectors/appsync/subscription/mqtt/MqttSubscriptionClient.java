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

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public MqttSubscriptionClient(Context applicationContext, String wssURL, String clientId) {
        mMqttAndroidClient = new MqttAndroidClient(applicationContext, wssURL, clientId, new MemoryPersistence());
        subsMap = new HashMap<>();
    }

    @Override
    public void connect(final SubscriptionClientCallback callback) {
        try {
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setKeepAliveInterval(5);
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
            if (msgListener == null) {
                msgListener = new MessageListener();
                msgListener.setCallback(callback);
                msgListener.setTransmitting(true);
                mMqttAndroidClient.subscribe(topic, qos, msgListener);
            } else {
                mMqttAndroidClient.subscribe(topic, qos, msgListener);
            }
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
    }

    @Override
    public void close() {
        final String clientRepresentation = mMqttAndroidClient.toString();
        try {
            mMqttAndroidClient.disconnect();
            mMqttAndroidClient.close();
        } catch (Exception e) {
            Log.w(TAG, "Closing " + clientRepresentation + "mqtt client", e);
        }
    }

    class MessageListener implements IMqttMessageListener {
        SubscriptionCallback callback;
        private boolean isTransmitting;

        public void setCallback(SubscriptionCallback callback) {
            this.callback = callback;
        }

        public void setTransmitting(boolean truth) {
            this.isTransmitting = truth;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d("", "transmit: " + isTransmitting + "mqttL: " + this + "subL: " + callback + " Topic: " + topic + " Msg: " + message.toString());
            if (isTransmitting) {
                callback.onMessage(topic, message.toString());
            }
        }
    }
}
