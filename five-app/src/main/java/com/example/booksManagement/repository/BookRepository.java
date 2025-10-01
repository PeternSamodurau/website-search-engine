package com.example.booksManagement.repository;

import com.example.booksManagement.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByTitle(String title);

    Optional<Book> findByTitleAndAuthor(String title, String author);

    List<Book> findAllByCategoryName(String categoryName);
}
