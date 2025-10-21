package com.example.springbootnewsportal.exception;

import com.example.springbootnewsportal.dto.response.ErrorResponseDTO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException; // ДОБАВЛЕНО

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Логика для обработки ошибок валидации
        return new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), "Validation error");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Optional<String> customMessage = extractDuplicateEntryMessage(ex);
        if (customMessage.isPresent()) {
            log.warn("Data integrity violation: {}", customMessage.get());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(HttpStatus.CONFLICT.value(), customMessage.get()));
        }
        log.error("An unexpected data integrity violation occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected database error occurred."));
    }

    @ExceptionHandler(DuplicateNewsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleDuplicateNewsException(DuplicateNewsException ex) {
        log.warn("Attempt to create duplicate news: {}", ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(DuplicateCommentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleDuplicateCommentException(DuplicateCommentException ex) {
        log.warn("Attempt to create duplicate comment: {}", ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    // ДОБАВЛЕНО: Обработчик для org.springframework.security.access.AccessDeniedException
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDTO handleSpringSecurityAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied (403): {}", ex.getMessage());
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

    // для парсинга сообщения об ошибке
    private Optional<String> extractDuplicateEntryMessage(DataIntegrityViolationException ex) {
        String causeMessage = ex.getMostSpecificCause().getMessage();

        if (causeMessage != null && causeMessage.contains("violates unique constraint")) {
            Pattern pattern = Pattern.compile("Key \\(([^)]+)\\)=\\(([^)]+)\\) already exists");
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