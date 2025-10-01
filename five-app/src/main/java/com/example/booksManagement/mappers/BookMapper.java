package com.example.booksManagement.mappers;

import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    // Метод toResponse остается без изменений
    public BookResponse toResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .categoryName(book.getCategory() != null ? book.getCategory().getName() : null)
                .build();
    }

    // А вот toEntity нужно исправить
    public Book toEntity(UserBookRequest request) {
        // Создаем "пустую" категорию, устанавливая в нее только имя из запроса.
        // BookService сам решит, найти существующую по этому имени или создать новую.
        Category category = Category.builder()
                .name(request.getCategoryName())
                .build();

        return Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .category(category) // Передаем "неполную" категорию дальше в сервис
                .build();
    }
}