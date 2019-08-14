package com.amazonaws.mobileconnectors.appsync;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown by AWSAppSyncClient#clearCaches() when there
 * is an error thrown during the clear caches operation.
 */
public class ClearCacheException extends AWSAppSyncClientException {
    /**
     * Default constructor.
     */
    public ClearCacheException() {
        super();
    }

    /**
     * @param message the exception message.
     */
    public ClearCacheException(final String message) {
        super(message);
    }

    /**
     * @param message the exception message.
     * @param cause the throwable object that contains the cause.
     */
    public ClearCacheException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause the throwable object that contains the cause
     */
    public ClearCacheException(final Throwable cause) {
        super(cause);
    }

    private List<Exception> exceptions;

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public void addException(Exception exception) {
        if (exceptions  == null) {
            exceptions = new ArrayList<Exception>();
        }
        exceptions.add(exception);
    }
}
