package com.amazonaws.mobileconnectors.appsync.subscription;

/**
  Indicates if a subscription was disconnected.
 */
public class SubscriptionDisconnectedException extends Exception {

    public SubscriptionDisconnectedException(String message) {
        super(message);
    }

    public SubscriptionDisconnectedException(String message, Throwable cause) {
        super(message, cause);
    }

}
