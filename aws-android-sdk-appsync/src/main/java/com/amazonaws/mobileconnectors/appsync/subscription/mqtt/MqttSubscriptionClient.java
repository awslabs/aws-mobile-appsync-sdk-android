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

        //Setup message Listener at the connection level.
        msgListener = new MessageListener();
        msgListener.client = this;
        msgListener.setClientID(clientId);
        setTransmitting(false);

        //Setup connection status Listener
        clientConnectionListener = new ClientConnectionListener();
        clientConnectionListener.setClientID(clientId);
    }

    @Override
    public void connect(final SubscriptionClientCallback callback) {
        try {
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setAutomaticReconnect(false);
            mqttConnectOptions.setKeepAliveInterval(30);
            clientConnectionListener.setCallback(callback);
            mMqttAndroidClient.setCallback(clientConnectionListener);
            Log.v(TAG, "Subscription Infrastructure: Calling MQTT Connect with actual endpoint for client ID[" + mMqttAndroidClient.getClientId() + "]");
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
            Log.e("TAG", "Subscription Infrastructure: Failed to connect mqtt client for clientID [" + mMqttAndroidClient.getClientId() + "]" , e);
            callback.onError(e);
        }
    }

    @Override
    public void subscribe(final String topic, int qos, final SubscriptionCallback callback) {
        try {
            Log.d(TAG, this + "Subscription Infrastructure: Attempt to subscribe to topic " + topic + " on clientID [" + mMqttAndroidClient.getClientId() + "]");
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
        if (clientConnectionListener != null) {
            clientConnectionListener.setTransmitting(isTransmitting);
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
                            Log.d(TAG, "Subscription Infrastructure: Successfully closed the connection. Client ID [" + mMqttAndroidClient.getClientId() + "]");
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.w(TAG, "Subscription Infrastructure: Got exception [" + exception +"] when attempting to disconnect clientID " + mMqttAndroidClient.getClientId() + "]" );
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Closing " + clientRepresentation + " mqtt client [" + mMqttAndroidClient.getClientId() + "]", e);
        }
    }

    class MessageListener implements IMqttMessageListener {
        public MqttSubscriptionClient client;
        SubscriptionCallback callback;
        private boolean isTransmitting;
        private String clientID;

        public void setCallback(SubscriptionCallback callback) {
            this.callback = callback;
        }


        public void setClientID(String clientID) {
            this.clientID = clientID;
        }

        public void setTransmitting(boolean truth) {
            Log.v(TAG, "Subscription Infrastructure: Set Message transmitting to " + truth + " for client [" + clientID + "]");
            this.isTransmitting = truth;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.v(TAG, "Subscription Infrastructure: Received message on client ["  + clientID + "]");
            if (isTransmitting) {
                Log.v(TAG, "Subscription Infrastructure: Transmitting message from client ["  + clientID + "] mqttL: " + this + "subL: " + callback + " Topic: " + topic + " Msg: " + message.toString());
                callback.onMessage(topic, message.toString());
            }
        }
    }

    class ClientConnectionListener implements MqttCallback {
        private boolean isTransmitting;
        SubscriptionClientCallback callback;
        private String clientID;


        public ClientConnectionListener( ) {
            isTransmitting = true;
        }

        public void setCallback(SubscriptionClientCallback callback) {
            this.callback = callback;
        }

        public void setClientID(String clientID) {
            this.clientID = clientID;
        }

        public void setTransmitting(boolean truth) {
            Log.v(TAG, "Subscription Infrastructure: Set Connection transmitting to " + truth + " for client [" + clientID + "]");
            this.isTransmitting = truth;
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.v(TAG, "Subscription Infrastructure: client connection lost for client [" + clientID + "]");
            if (isTransmitting) {
                Log.v(TAG, "Subscription Infrastructure: Transmitting client connection lost for client [" + clientID +  "]");
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
