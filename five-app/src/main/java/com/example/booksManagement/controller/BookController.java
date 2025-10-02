package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.ApiResponse;
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.mappers.BookMapper;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit; // <-- ДОБАВЛЕН ИМПОРТ
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
    @Operation(summary = "Получить список всех книг", description = "Возвращает полный список всех книг, существующих в базе данных.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список книг успешно получен")
    })
    public ResponseEntity<ApiResponse<List<BookResponse>>> getAllBooks() {
        log.info("Request to get all books");

        long startTime = System.nanoTime();
        List<Book> books = bookService.findAll();
        long durationInNanos = System.nanoTime() - startTime;
        log.info("bookService.findAll() executed in {} ms", TimeUnit.NANOSECONDS.toMillis(durationInNanos));

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
    @Operation(summary = "Получить книгу по ID", description = "Возвращает одну книгу по её уникальному идентификатору.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Книга найдена"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Книга с таким ID не найдена", content = @Content)
    })
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(
            @Parameter(description = "Уникальный идентификатор книги", required = true) @PathVariable Long id) {
        log.info("Request to get book by id: {}", id);

        long startTime = System.nanoTime();
        Book book = bookService.findById(id);
        long durationInNanos = System.nanoTime() - startTime;
        log.info("bookService.findById() for id {} executed in {} ms", id, TimeUnit.NANOSECONDS.toMillis(durationInNanos));

        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Book successfully retrieved",
                bookMapper.toResponse(book)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Создать новую книгу", description = "Создает новую книгу. Если указана категория, которая уже существует, она будет переиспользована. Если нет - будет создана новая.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Книга успешно создана"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Книга с таким названием уже существует", content = @Content)
    })
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@RequestBody UserBookRequest request) {
        log.info("Request to create new book: {}", request);

        long startTime = System.nanoTime();
        Book newBook = bookService.save(bookMapper.toEntity(request));
        long durationInNanos = System.nanoTime() - startTime;
        log.info("bookService.save() executed in {} ms", TimeUnit.NANOSECONDS.toMillis(durationInNanos));

        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Book successfully created",
                bookMapper.toResponse(newBook)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующую книгу по ID", description = "Обновляет данные существующей книги по её ID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Книга успешно обновлена"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Книга с таким ID не найдена", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Другая книга уже имеет такое название", content = @Content)
    })
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @Parameter(description = "ID книги, которую нужно обновить", required = true) @PathVariable Long id,
            @RequestBody UserBookRequest request) {
        log.info("Request to update book with id: {}. New data: {}", id, request);
        Book bookToUpdate = bookMapper.toEntity(request);
        bookToUpdate.setId(id);

        long startTime = System.nanoTime();
        Book updatedBook = bookService.update(bookToUpdate);
        long durationInNanos = System.nanoTime() - startTime;
        log.info("bookService.update() for id {} executed in {} ms", id, TimeUnit.NANOSECONDS.toMillis(durationInNanos));

        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Book successfully updated",
                bookMapper.toResponse(updatedBook)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить книгу по ID", description = "Удаляет книгу из базы данных по её ID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Книга успешно удалена", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Книга с таким ID не найдена", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @Parameter(description = "ID книги, которую нужно удалить", required = true) @PathVariable Long id) {
        log.info("Request to delete book with id: {}", id);

        long startTime = System.nanoTime();
        bookService.deleteById(id);
        long durationInNanos = System.nanoTime() - startTime;
        log.info("bookService.deleteById() for id {} executed in {} ms", id, TimeUnit.NANOSECONDS.toMillis(durationInNanos));

        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NO_CONTENT.value(),
                "Book successfully deleted"
        );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Найти книгу по названию и автору", description = "Возвращает одну книгу при точном совпадении названия и автора.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Книга найдена"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Книга с таким названием и автором не найдена", content = @Content)
    })
    public ResponseEntity<ApiResponse<BookResponse>> getBookByTitleAndAuthor(
            @Parameter(description = "Название книги для поиска", required = true, in = ParameterIn.QUERY) @RequestParam String title,
            @Parameter(description = "Автор книги для поиска", required = true, in = ParameterIn.QUERY) @RequestParam String author) {
        log.info("Request to search book by title: '{}' and author: '{}'", title, author);

        long startTime = System.nanoTime();
        Book book = bookService.findByTitleAndAuthor(title, author);
        long durationInNanos = System.nanoTime() - startTime;
        log.info("bookService.findByTitleAndAuthor() executed in {} ms", TimeUnit.NANOSECONDS.toMillis(durationInNanos));

        ApiResponse<BookResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Book successfully found by title and author",
                bookMapper.toResponse(book)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{categoryName}")
    @Operation(summary = "Найти все книги по названию категории", description = "Возвращает список всех книг, относящихся к указанной категории.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список книг успешно получен"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Категория с таким названием не найдена", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<BookResponse>>> getBooksByCategoryName(
            @Parameter(description = "Название категории для поиска книг", required = true) @PathVariable String categoryName) {
        log.info("Request to get books by category: '{}'", categoryName);

        long startTime = System.nanoTime();
        List<Book> books = bookService.findAllByCategoryName(categoryName);
        long durationInNanos = System.nanoTime() - startTime;
        log.info("bookService.findAllByCategoryName() for category '{}' executed in {} ms", categoryName, TimeUnit.NANOSECONDS.toMillis(durationInNanos));

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