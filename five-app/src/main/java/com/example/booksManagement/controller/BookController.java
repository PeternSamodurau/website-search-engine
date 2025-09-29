package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.service.BookService;
import com.example.booksManagement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<BookResponse> findByTitleAndAuthor(@RequestParam String title, @RequestParam String author) {
        Book book = bookService.findByTitleAndAuthor(title, author);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new BookResponse(book));
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<BookResponse>> findAllByCategoryName(@PathVariable String categoryName) {
        List<Book> books = bookService.findAllByCategoryName(categoryName);
        if (books.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<BookResponse> bookResponses = books.stream().map(BookResponse::new).collect(Collectors.toList());
        return ResponseEntity.ok(bookResponses);
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@RequestBody UserBookRequest request) {
        Category category = categoryService.findByName(request.getCategoryName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + request.getCategoryName()));

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(category);
        Book savedBook = bookService.save(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BookResponse(savedBook));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> update(@PathVariable Long id, @RequestBody UserBookRequest request) {
        Book book = bookService.findById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        Category category = categoryService.findByName(request.getCategoryName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + request.getCategoryName()));

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(category);
        Book updatedBook = bookService.update(book);
        return ResponseEntity.ok(new BookResponse(updatedBook));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
