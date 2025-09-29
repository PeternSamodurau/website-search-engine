package com.example.booksManagement.service;

import com.example.booksManagement.model.Book;

import java.util.List;

public interface BookService {
    List<Book> findAll();

    Book findById(Long id);

    Book findByTitleAndAuthor(String title, String author);

    List<Book> findAllByCategoryName(String categoryName);

    Book save(Book book);

    Book update(Book book);

    void deleteById(Long id);
}
