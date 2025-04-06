package org.horikita.exceptions;

public class NonRetriableException extends Exception {

    public NonRetriableException(String message) {
        super(message);
    }

    public NonRetriableException(String message, Throwable cause) {
        super(message, cause);
    }
}
