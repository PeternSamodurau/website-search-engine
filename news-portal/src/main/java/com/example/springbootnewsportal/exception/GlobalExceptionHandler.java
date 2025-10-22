package com.example.springbootnewsportal.exception;

import com.example.springbootnewsportal.dto.response.ErrorResponseDTO;
import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleResourceNotFoundException(ResourceNotFoundException ex) {
        String userMessage = "Запрашиваемый ресурс не найден.";
        log.error("Ресурс не найден (404): {}", userMessage);
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), userMessage);
    }

    // Обработчики для CategoryAlreadyExistsException и UserAlreadyExistsException удалены,
    // так как эти исключения не найдены в проекте.

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        String userMessage = "Ошибка валидации входных данных.";
        log.error("Ошибка валидации (400): {}. Подробности: {}", userMessage, errors.toString());
        return new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), userMessage);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleConstraintViolationException(ConstraintViolationException ex) {
        String userMessage = "Нарушение ограничений данных.";
        log.error("Нарушение ограничения (400): {}. Подробности: {}", userMessage, ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), userMessage);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDTO handleSpringSecurityAccessDeniedException(AccessDeniedException ex) {
        String userMessage = "Доступ запрещен. У вас нет необходимых прав.";
        log.error("Доступ запрещен (403): {}", userMessage);
        return new ErrorResponseDTO(HttpStatus.FORBIDDEN.value(), userMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleAllUncaughtException(Exception ex) {
        String userMessage = "Произошла внутренняя ошибка сервера.";
        log.error("Внутренняя ошибка сервера (500): {}. Детали: {}", userMessage, ex.getMessage(), ex);
        return new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), userMessage);
    }
}