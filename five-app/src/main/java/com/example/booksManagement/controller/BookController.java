package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.mappers.BookMapper;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
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
    private final BookMapper bookMapper;

    @Operation(summary = "Поиск книги по названию и автору")
    @GetMapping("/search")
    public ResponseEntity<BookResponse> getBookByTitleAndAuthor(@RequestParam String title, @RequestParam String author) {
        Book book = bookService.findByTitleAndAuthor(title, author);
        return ResponseEntity.ok(bookMapper.toResponse(book));
    }

    @Operation(summary = "Получение списка книг по названию категории")
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<BookResponse>> getBooksByCategoryName(@PathVariable String categoryName) {
        List<BookResponse> responses = bookService.findAllByCategoryName(categoryName)
                .stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Получение книги по ID")
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        Book book = bookService.findById(id);
        return ResponseEntity.ok(bookMapper.toResponse(book));
    }

    @Operation(summary = "Создание новой книги")
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@RequestBody UserBookRequest request) {
        Book newBook = bookMapper.toEntity(request);
        Book savedBook = bookService.save(newBook);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookMapper.toResponse(savedBook));
    }

    @Operation(summary = "Обновление существующей книги по ID")
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @RequestBody UserBookRequest request) {
        Book bookToUpdate = bookMapper.toEntity(request);
        bookToUpdate.setId(id);
        Book updatedBook = bookService.update(bookToUpdate);
        return ResponseEntity.ok(bookMapper.toResponse(updatedBook));
    }

    @Operation(summary = "Удаление книги по ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
