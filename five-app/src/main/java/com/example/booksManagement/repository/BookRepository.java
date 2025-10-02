package com.example.booksManagement.repository;

import com.example.booksManagement.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByTitleAndAuthor(String title, String author);
    List<Book> findAllByCategoryName(String categoryName);
    boolean existsByTitleAndAuthor(String title, String author); // <-- ЭТА СТРОКА ДОБАВЛЕНА
}