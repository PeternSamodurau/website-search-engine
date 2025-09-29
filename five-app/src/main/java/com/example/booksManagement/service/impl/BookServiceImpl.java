package com.example.booksManagement.service.impl;

import com.example.booksManagement.model.Book;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.service.BookService;
import com.example.booksManagement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryService categoryService;

    @Override
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Book findById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    @Override
    @Cacheable(value = "book", key = "#title + #author")
    public Book findByTitleAndAuthor(String title, String author) {
        return bookRepository.findByTitleAndAuthor(title, author).orElse(null);
    }

    @Override
    @Cacheable(value = "booksByCategory", key = "#categoryName")
    public List<Book> findAllByCategoryName(String categoryName) {
        return bookRepository.findAllByCategoryName(categoryName);
    }

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    @Override
    @CacheEvict(value = {"book", "booksByCategory"}, allEntries = true)
    public Book update(Book book) {
        return bookRepository.save(book);
    }

    @Override
    @CacheEvict(value = {"book", "booksByCategory"}, allEntries = true)
    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }
}
