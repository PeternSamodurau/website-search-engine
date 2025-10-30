package com.example.seven_app.exception;

public class SelfDeletionException extends RuntimeException {
    public SelfDeletionException(String message) {
        super(message);
    }
}
