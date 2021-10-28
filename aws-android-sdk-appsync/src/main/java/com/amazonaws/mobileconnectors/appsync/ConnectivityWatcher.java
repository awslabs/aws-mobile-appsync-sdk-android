package com.amazonaws.mobileconnectors.appsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.RequiresApi;

class ConnectivityWatcher {

    interface Callback {
        void onConnectivityChanged(boolean isNetworkConnected);
    }

    private final Context context;
    private final Callback callback;
    private final ConnectivityManager connManager;
    private BroadcastReceiver broadcastReceiver;
    private ConnectivityManager.NetworkCallback networkCallback;

    ConnectivityWatcher(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    void register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerNetworkCallback();
        } else {
            registerBroadcastReceiver();
        }
    }

    void unregister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            unregisterNetworkCallback();
        } else {
            unregisterBroadcastReceiver();
        }
    }

    private void registerBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new ConnectivityChangeReceiver();
            context.registerReceiver(broadcastReceiver, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            context.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void registerNetworkCallback() {
        if (networkCallback == null) {
            networkCallback = new NetworkCallback();
            connManager.registerNetworkCallback(
                    new NetworkRequest.Builder().build(), networkCallback);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            connManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    /**
     * Gets the status of network connectivity.
     *
     * @return true if network is connected, false otherwise.
     */
    private boolean isNetworkConnected() {
        final NetworkInfo info = connManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private class ConnectivityChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                callback.onConnectivityChanged(isNetworkConnected());
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private boolean isConnected = isNetworkConnected();

        private void checkConnected() {
            boolean isConnectedNow = isNetworkConnected();
            if (isConnected != isConnectedNow) {
                isConnected = isConnectedNow;
                callback.onConnectivityChanged(isConnectedNow);
            }
        }

        @Override
        public void onAvailable(Network network) {
            checkConnected();
        }

        @Override
        public void onLost(Network network) {
            checkConnected();
        }
    }

}
