package com.example.seven_app.exception;

import com.example.seven_app.dto.response.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({UserNotFoundException.class, TaskNotFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleNotFoundException(RuntimeException ex) {
        log.error("Not Found Error: {}", ex.getMessage());
        ErrorResponseDto errorResponse = new ErrorResponseDto(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Обработчик ошибок валидации (неверный формат email)
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(WebExchangeBindException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.error("Validation Error: {}", errorMessage);
        ErrorResponseDto errorResponse = new ErrorResponseDto(errorMessage);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // Обработчик ошибок уникальности и других статусов
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException ex) {
        // НЕ ЛОГИРУЕМ ожидаемую ошибку о дубликате, чтобы не засорять логи
        if (ex.getStatusCode() != HttpStatus.CONFLICT) {
            log.error("Service Error: Status={}, Reason='{}'", ex.getStatusCode(), ex.getReason());
        }
        ErrorResponseDto errorResponse = new ErrorResponseDto(ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }
}
