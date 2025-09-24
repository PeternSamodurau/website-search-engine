package com.example.springbootnewsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateNewsException extends RuntimeException {

    public DuplicateNewsException(String message) {
        super(message);
    }
}
