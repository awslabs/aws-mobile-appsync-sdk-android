package com.amazonaws.mobileconnectors.appsync;

import com.amazonaws.apollographql.apollo.exception.ApolloException;

public class ConflictResolutionFailedException extends ApolloException {
    public ConflictResolutionFailedException(String message) {
        super(message);
    }

    public ConflictResolutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}


