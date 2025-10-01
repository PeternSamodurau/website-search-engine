package com.example.booksManagement.exception;

/**
 * Исключение, которое выбрасывается, когда сущность (например, книга или категория)
 * не найдена в базе данных.
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}