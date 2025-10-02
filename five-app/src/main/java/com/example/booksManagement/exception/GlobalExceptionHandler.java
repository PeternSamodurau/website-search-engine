package com.example.booksManagement.exception;

import com.example.booksManagement.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("404 NOT_FOUND: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("409 CONFLICT: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // --- НОВЫЙ ОБРАБОТЧИК ---
    @ExceptionHandler(CustomFeignException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomFeignException(CustomFeignException ex) {
        // Логируем ошибку от внешнего сервиса
        log.error("502 BAD_GATEWAY: Error from external API. Status: {}, Message: {}", ex.getStatus(), ex.getMessage());

        // Формируем понятный ответ для клиента
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.BAD_GATEWAY.value(),
                "The external book service is currently unavailable. Please try again later."
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }
    // -------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("500 INTERNAL_SERVER_ERROR: An unexpected error occurred", ex);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An internal server error occurred. Please contact support."
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}