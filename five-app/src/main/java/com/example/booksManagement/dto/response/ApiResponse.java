package com.example.booksManagement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Универсальный класс-обертка для всех ответов API.
 * @param <T> Тип данных, которые будут возвращены в поле 'data'.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Поле 'data' не будет включено в JSON, если оно равно null.
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;

    /**
     * Конструктор для ответов, не содержащих данных (например, при удалении).
     * @param status HTTP-статус ответа.
     * @param message Сообщение, описывающее результат.
     */
    public ApiResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}