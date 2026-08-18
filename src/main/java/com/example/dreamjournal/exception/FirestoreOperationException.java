package com.example.dreamjournal.exception;

public class FirestoreOperationException extends RuntimeException {

    public FirestoreOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
