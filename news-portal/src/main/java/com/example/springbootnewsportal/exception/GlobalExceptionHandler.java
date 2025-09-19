package com.example.springbootnewsportal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Обработчик для ошибок валидации DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("Validation error: {}", errorMessage);
        return new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), "Validation Error: " + errorMessage);
    }

    // НОВЫЙ ОБРАБОТЧИК для ошибок уникальности от БД
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = extractDuplicateEntryMessage(ex).orElse("Data integrity violation");
        log.error("Data integrity violation: {}", message);
        return new ErrorResponseDTO(HttpStatus.CONFLICT.value(), message);
    }

    // ВАШИ СУЩЕСТВУЮЩИЕ ОБРАБОТЧИКИ
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleDuplicateResourceException(DuplicateResourceException ex) {
        log.error("Duplicate resource conflict: {}", ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDTO handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handlePropertyReferenceException(PropertyReferenceException ex) {
        log.error("Invalid request parameter. Sorting property not found: {}", ex.getPropertyName());
        return new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), "Invalid sorting parameter: " + ex.getPropertyName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleAllUncaughtException(Exception ex) {
        log.error("An unexpected internal server error occurred", ex);
        return new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred. Please contact support.");
    }

    // НОВЫЙ ПРИВАТНЫЙ МЕТОД для парсинга сообщения об ошибке
    private Optional<String> extractDuplicateEntryMessage(DataIntegrityViolationException ex) {
        String causeMessage = ex.getMostSpecificCause().getMessage();
        if (causeMessage != null && causeMessage.contains("violates unique constraint")) {
            // Пример сообщения: ERROR: duplicate key value violates unique constraint "users_email_key"
            // Подробности: Key (email)=(test@example.com) already exists.
            Pattern pattern = Pattern.compile("Key \\((.*?)\\)=\\((.*?)\\) already exists");
            Matcher matcher = pattern.matcher(causeMessage);
            if (matcher.find()) {
                String field = matcher.group(1);
                String value = matcher.group(2);
                return Optional.of(String.format("Entry with '%s' = '%s' already exists.", field, value));
            }
        }
        return Optional.empty();
    }
}
