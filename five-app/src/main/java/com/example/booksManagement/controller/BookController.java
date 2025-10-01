package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.mappers.BookMapper;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // Убедитесь, что этот импорт есть
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "API по работе с книгами", description = "Контроллер для выполнения CRUD-операций с книгами и их поиска")
public class BookController {

    private final BookService bookService;
    private final BookMapper bookMapper;

    @GetMapping
    @Operation(summary = "Получить список всех книг")
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        List<Book> books = bookService.findAll();
        return ResponseEntity.ok(books.stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить книгу по ID")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        Book book = bookService.findById(id);
        return ResponseEntity.ok(bookMapper.toResponse(book));
    }

    @PostMapping
    @Operation(summary = "Создать новую книгу")
    public ResponseEntity<BookResponse> createBook(@RequestBody UserBookRequest request) {
        Book newBook = bookService.save(bookMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(bookMapper.toResponse(newBook));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующую книгу по ID")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @RequestBody UserBookRequest request) {
        Book bookToUpdate = bookMapper.toEntity(request);
        bookToUpdate.setId(id); // Устанавливаем ID из пути
        Book updatedBook = bookService.update(bookToUpdate);
        return ResponseEntity.ok(bookMapper.toResponse(updatedBook));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить книгу по ID")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Найти книгу по названию и автору")
    public ResponseEntity<BookResponse> getBookByTitleAndAuthor(@RequestParam String title, @RequestParam String author) {
        Book book = bookService.findByTitleAndAuthor(title, author);
        return ResponseEntity.ok(bookMapper.toResponse(book));
    }

    @GetMapping("/category/{categoryName}")
    @Operation(summary = "Найти все книги по названию категории")
    public ResponseEntity<List<BookResponse>> getBooksByCategoryName(@PathVariable String categoryName) {
        List<Book> books = bookService.findAllByCategoryName(categoryName);
        return ResponseEntity.ok(books.stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList()));
    }
}