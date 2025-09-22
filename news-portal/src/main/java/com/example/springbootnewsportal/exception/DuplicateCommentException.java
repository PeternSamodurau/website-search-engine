package com.example.springbootnewsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // Статус 409 Conflict - идеально подходит для дубликатов
public class DuplicateCommentException extends RuntimeException {

    public DuplicateCommentException(String message) {
        super(message);
    }
}
