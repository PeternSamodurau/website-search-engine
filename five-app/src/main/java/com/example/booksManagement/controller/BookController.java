package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.ApiResponse; // <- ИМПОРТ
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.mappers.BookMapper;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "API по работе с книгами", description = "Контроллер для выполнения CRUD-операций с книгами и их поиска")
@Slf4j
public class BookController {

    private final BookService bookService;
    private final BookMapper bookMapper;

    @GetMapping
    @Operation(summary = "Получить список всех книг")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getAllBooks() {
        log.info("Request to get all books");
        List<Book> books = bookService.findAll();
        List<BookResponse> bookResponses = books.stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList());

        ApiResponse<List<BookResponse>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Successfully retrieved all books",
                bookResponses
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить книгу по ID")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long id) {
        log.info("Request to get book by id: {}", id);
        Book book = bookService.findById(id);
        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Book successfully retrieved",
                bookMapper.toResponse(book)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Создать новую книгу")
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@RequestBody UserBookRequest request) {
        log.info("Request to create new book: {}", request);
        Book newBook = bookService.save(bookMapper.toEntity(request));
        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Book successfully created",
                bookMapper.toResponse(newBook)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующую книгу по ID")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(@PathVariable Long id, @RequestBody UserBookRequest request) {
        log.info("Request to update book with id: {}. New data: {}", id, request);
        Book bookToUpdate = bookMapper.toEntity(request);
        bookToUpdate.setId(id);
        Book updatedBook = bookService.update(bookToUpdate);
        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Book successfully updated",
                bookMapper.toResponse(updatedBook)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить книгу по ID, категория не удалится")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        log.info("Request to delete book with id: {}", id);
        bookService.deleteById(id);
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NO_CONTENT.value(),
                "Book successfully deleted"
        );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Найти книгу по названию и автору")
    public ResponseEntity<ApiResponse<BookResponse>> getBookByTitleAndAuthor(@RequestParam String title, @RequestParam String author) {
        log.info("Request to search book by title: '{}' and author: '{}'", title, author);
        Book book = bookService.findByTitleAndAuthor(title, author);
        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Book successfully found by title and author",
                bookMapper.toResponse(book)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{categoryName}")
    @Operation(summary = "Найти все книги по названию категории")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getBooksByCategoryName(@PathVariable String categoryName) {
        log.info("Request to get books by category: '{}'", categoryName);
        List<Book> books = bookService.findAllByCategoryName(categoryName);
        List<BookResponse> bookResponses = books.stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList());

        ApiResponse<List<BookResponse>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Successfully retrieved books by category",
                bookResponses
        );
        return ResponseEntity.ok(response);
    }
}