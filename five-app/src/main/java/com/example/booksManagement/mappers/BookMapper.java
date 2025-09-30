package com.example.booksManagement.mappers;


import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    /**
     * Преобразует сущность Book в BookResponse DTO.
     * @param book Сущность для преобразования.
     * @return BookResponse DTO.
     */
    public BookResponse toResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        if (book.getCategory() != null) {
            response.setCategoryName(book.getCategory().getName());
        }
        return response;
    }

    /**
     * Создает новую сущность Book из UpsertBookRequest DTO.
     * @param request DTO с данными для создания.
     * @param category Сущность категории для связи.
     * @return Новая сущность Book.
     */
    public Book toEntity(UserBookRequest request, Category category) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(category);
        return book;
    }

    /**
     * Обновляет существующую сущность Book данными из UpsertBookRequest DTO.
     * @param request DTO с новыми данными.
     * @param book Существующая сущность для обновления.
     * @param category Сущность категории для связи.
     */
    public void updateEntity(UserBookRequest request, Book book, Category category) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(category);
    }
}
