package com.example.booksManagement.dto.response;

import com.example.booksManagement.model.Book;
import lombok.Data;

@Data
public class BookResponse {
    private Long id;
    private String title;
    private String author;
    private String categoryName;

    public BookResponse(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.categoryName = book.getCategory().getName();
    }
}
