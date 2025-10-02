package com.example.booksManagement.service.impl;

import com.example.booksManagement.exception.DuplicateResourceException;
import com.example.booksManagement.exception.EntityNotFoundException;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.repository.CategoryRepository;
import com.example.booksManagement.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book with id " + id + " not found"));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookByTitleAndAuthor", allEntries = true),
            @CacheEvict(value = "booksByCategory", allEntries = true)
    })
    public Book save(Book book) {
        if (bookRepository.existsByTitleAndAuthor(book.getTitle(), book.getAuthor())) {
            throw new DuplicateResourceException("Book with title " + book.getTitle() + " and author " + book.getAuthor() + " already exists");
        }
        return saveBookWithCategory(book);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookByTitleAndAuthor", allEntries = true),
            @CacheEvict(value = "booksByCategory", allEntries = true)
    })
    public Book update(Book book) {
        // Для update предполагаем, что id уже установлен в объекте book
        Book existingBook = bookRepository.findById(book.getId())
                .orElseThrow(() -> new EntityNotFoundException("Book with id " + book.getId() + " not found"));

        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setCategory(book.getCategory()); // Обновляем и категорию

        return saveBookWithCategory(existingBook);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookByTitleAndAuthor", allEntries = true),
            @CacheEvict(value = "booksByCategory", allEntries = true)
    })
    public void deleteById(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new EntityNotFoundException("Book with id " + id + " not found");
        }
        bookRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "bookByTitleAndAuthor", key = "#title + #author")
    public Book findByTitleAndAuthor(String title, String author) {
        return bookRepository.findByTitleAndAuthor(title, author)
                .orElseThrow(() -> new EntityNotFoundException("Book with title " + title + " and author " + author + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "booksByCategory", key = "#categoryName")
    public List<Book> findAllByCategoryName(String categoryName) {
        return bookRepository.findAllByCategoryName(categoryName);
    }

    private Book saveBookWithCategory(Book book) {
        Category category = book.getCategory();
        if (category != null && category.getName() != null) {
            Optional<Category> existingCategory = categoryRepository.findByName(category.getName());
            if (existingCategory.isPresent()) {
                book.setCategory(existingCategory.get());
            } else {
                // Если категория новая, ее нужно сначала сохранить
                Category newCategory = categoryRepository.save(category);
                book.setCategory(newCategory);
            }
        }
        return bookRepository.save(book);
    }
}