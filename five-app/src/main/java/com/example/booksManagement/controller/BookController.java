package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UserBookRequest; // ИСПРАВЛЕНО
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.mappers.BookMapper;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.service.BookService;
import com.example.booksManagement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final BookMapper bookMapper;

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        List<BookResponse> responses = bookService.findAll()
                .stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        // ИСПРАВЛЕНО: .orElseThrow() больше не нужен. Сервис сам выбрасывает исключение.
        Book book = bookService.findById(id);
        return ResponseEntity.ok(bookMapper.toResponse(book));
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@RequestBody UserBookRequest request) { // ИСПРАВЛЕНО
        // ИСПРАВЛЕНО: .orElseThrow() больше не нужен.
        Category category = categoryService.findByName(request.getCategoryName());

        Book newBook = bookMapper.toEntity(request, category);
        Book savedBook = bookService.save(newBook);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookMapper.toResponse(savedBook));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @RequestBody UserBookRequest request) { // ИСПРАВЛЕНО
        // ИСПРАВЛЕНО: .orElseThrow() больше не нужен.
        Category category = categoryService.findByName(request.getCategoryName());
        Book book = bookService.findById(id); // Находим книгу (сервис проверит существование)

        bookMapper.updateEntity(request, book, category); // Обновляем поля

        Book updatedBook = bookService.update(book); // Сохраняем (сервис может добавить доп. логику)
        return ResponseEntity.ok(bookMapper.toResponse(updatedBook));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        // ИСПРАВЛЕНО: .isEmpty() больше не нужен. Сервис сам проверит существование.
        bookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}