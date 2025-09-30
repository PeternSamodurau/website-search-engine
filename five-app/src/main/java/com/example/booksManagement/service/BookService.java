package com.example.booksManagement.service;

import com.example.booksManagement.model.Book;
import java.util.List;
// Optional больше не нужен в сигнатурах публичных методов для контроллера
// import java.util.Optional;

public interface BookService {
    List<Book> findAll();

    /**
     * Находит книгу по ID.
     * @param id ID книги.
     * @return Найденную сущность Book.
     * @throws org.springframework.web.server.ResponseStatusException если книга не найдена.
     */
    Book findById(Long id);

    Book save(Book book);
    Book update(Book book);

    /**
     * Удаляет книгу по ID.
     * @param id ID книги.
     * @throws org.springframework.web.server.ResponseStatusException если книга не найдена.
     */
    void deleteById(Long id);
}