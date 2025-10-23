package com.example.springbootnewsportal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Ресурс не найден (404): {}", ex.getMessage());
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDTO handleAccessDeniedException(AccessDeniedException ex) {
        String userMessage = "Доступ запрещен.";
        log.error("Доступ запрещен (403): {}. Подробности: {}", userMessage, ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.FORBIDDEN.value(), userMessage);
    }

    @ExceptionHandler(DuplicateNewsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleDuplicateNewsException(DuplicateNewsException ex) {
        String userMessage = "Новость с таким заголовком уже существует.";
        log.error("Конфликт данных (409): {}. Подробности: {}", userMessage, ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.CONFLICT.value(), userMessage);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleEntityNotFoundException(EntityNotFoundException ex) {
        String userMessage = "Запрашиваемая сущность не найдена.";
        log.error("Сущность не найдена (404): {}. Подробности: {}", userMessage, ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), userMessage);
    }

    @ExceptionHandler(EntityExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleEntityExistsException(EntityExistsException ex) {
        String userMessage = "Сущность с такими данными уже существует.";
        log.error("Конфликт данных (409): {}. Подробности: {}", userMessage, ex.getMessage());
        return new ErrorResponseDTO(HttpStatus.CONFLICT.value(), userMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleAllUncaughtException(Exception ex) {
        String userMessage = "Произошла внутренняя ошибка сервера.";
        log.error("Внутренняя ошибка сервера (500): {}. Детали: {}", userMessage, ex.getMessage(), ex);
        return new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), userMessage);
    }
}