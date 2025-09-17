package com.example.springbootnewsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleResourceNotFound(ResourceNotFoundException ex) {
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDTO handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponseDTO(HttpStatus.FORBIDDEN.value(), "Access is denied. You do not have permission to perform this action.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), "Validation failed: " + errorMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleAllUncaughtException(Exception ex) {
        // В реальном приложении здесь должно быть логирование ошибки
        return new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred. Please contact support.");
    }
}
