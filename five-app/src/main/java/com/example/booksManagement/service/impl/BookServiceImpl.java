package com.example.booksManagement.service.impl;

import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.repository.CategoryRepository;
import com.example.booksManagement.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository; // Добавляем репозиторий категорий

    @Override
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Book findById(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found with ID: " + id));
    }

    @Override
    @Transactional
    public Book save(Book book) {
        // --- Начало логики "Найти или создать категорию" ---
        Category category = book.getCategory();
        // Проверяем, что у книги есть категория с названием
        if (category != null && category.getName() != null && !category.getName().isBlank()) {
            // Ищем категорию в БД по имени
            Category categoryFromDb = categoryRepository.findByName(category.getName())
                    .orElseGet(() -> {
                        // Если не нашли - создаем новую, сохраняем ее и возвращаем
                        return categoryRepository.save(Category.builder().name(category.getName()).build());
                    });
            // Устанавливаем в книгу категорию из БД (новую или уже существующую)
            book.setCategory(categoryFromDb);
        }
        // --- Конец логики ---

        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public Book update(Book book) {
        // Убедимся, что книга для обновления существует, чтобы не создать новую
        findById(book.getId());

        // Логика "Найти или создать категорию" - абсолютно такая же, как в методе save
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
        // Проверяем, что книга существует, перед удалением для более понятной ошибки
        findById(id);
        bookRepository.deleteById(id);
    }

    @Override
    public Book findByTitleAndAuthor(String title, String author) {
        return bookRepository.findByTitleAndAuthor(title, author)
                .orElseThrow(() -> new RuntimeException("Book not found with title: '" + title + "' and author: '" + author + "'"));
    }

    @Override
    public List<Book> findAllByCategoryName(String categoryName) {
        return bookRepository.findAllByCategoryName(categoryName);
    }
}