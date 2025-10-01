package com.example.booksManagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // Добавляем конструктор со всеми аргументами
@Builder // <- ДОБАВЛЯЕМ АННОТАЦИЮ BUILDER
public class BookResponse {
    private Long id;
    private String title;
    private String author;
    private String categoryName;
}