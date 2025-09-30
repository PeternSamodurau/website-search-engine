package com.example.booksManagement.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor; // Добавим для чистоты

@Data
@NoArgsConstructor // Я добавлю конструктор без аргументов, это хорошая практика для DTO
public class BookResponse {
    private Long id;
    private String title;
    private String author;
    private String categoryName;

    // КОНСТРУКТОР УДАЛЕН
}