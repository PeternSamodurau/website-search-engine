package com.example.springbootnewsportal.exception;

// Аннотация @ResponseStatus удалена, чтобы обработка передавалась в GlobalExceptionHandler
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
