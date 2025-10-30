package com.example.seven_app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException ex) {
        // Логируем то же сообщение, что и в ответе
        log.error("Пользователь с таким ID не найден: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of("message", "Пользователь с таким ID не найден"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTaskNotFoundException(TaskNotFoundException ex) {
        // Логируем то же сообщение, что и в ответе
        log.error("Задача с таким ID не найдена: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of("message", "Задача с таким ID не найдена"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        // Логируем то же сообщение, что и в ответе
        log.error("Пользователь с таким именем уже существует: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of("message", "Пользователь с таким именем уже существует"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        // Логируем то же сообщение, что и в ответе
        log.error("У вас нет прав доступа.");
        return new ResponseEntity<>(Map.of("message", "У вас нет прав доступа."), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenActionException(ForbiddenActionException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class) // <--- ДОБАВЛЕНО
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }
}
