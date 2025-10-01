package com.example.booksManagement.service.impl;

import com.example.booksManagement.exception.DuplicateResourceException;
import com.example.booksManagement.exception.EntityNotFoundException;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.repository.CategoryRepository;
import com.example.booksManagement.service.BookService;
import lombok.RequiredArgsConstructor;
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
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Book findById(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Book not found with ID: " + id));
    }

    @Override
    @Transactional
    public Book save(Book book) {
        // V-- ИЗМЕНЕНИЕ ЗДЕСЬ: Проверка на дубликат только по названию
        Optional<Book> existingBook = bookRepository.findByTitle(book.getTitle());
        if (existingBook.isPresent()) {
            throw new DuplicateResourceException("Book with title '" + book.getTitle() + "' already exists.");
        }

        Category category = book.getCategory();
        if (category != null && category.getName() != null && !category.getName().isBlank()) {
            Category categoryFromDb = categoryRepository.findByName(category.getName())
                    .orElseGet(() -> {
                        return categoryRepository.save(Category.builder().name(category.getName()).build());
                    });
            book.setCategory(categoryFromDb);
        }
        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public Book update(Book book) {
        findById(book.getId());

        Category category = book.getCategory();
        if (category != null && category.getName() != null && !category.getName().isBlank()) {
            Category categoryFromDb = categoryRepository.findByName(category.getName())
                    .orElseGet(() -> {
                        return categoryRepository.save(Category.builder().name(category.getName()).build());
                    });
            book.setCategory(categoryFromDb);
        }

        return bookRepository.save(book);
    }

    @Override
    public void deleteById(Long id) {
        findById(id);
        bookRepository.deleteById(id);
    }

    @Override
    public Book findByTitleAndAuthor(String title, String author) {
        return bookRepository.findByTitleAndAuthor(title, author)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with title: '" + title + "' and author: '" + author + "'"));
    }

    @Override
    public List<Book> findAllByCategoryName(String categoryName) {
        // V-- ИЗМЕНЕНИЕ ЗДЕСЬ: Добавлена проверка на существование категории
        categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with name: " + categoryName));

        return bookRepository.findAllByCategoryName(categoryName);
    }
}