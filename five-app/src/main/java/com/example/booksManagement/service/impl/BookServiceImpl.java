package com.example.booksManagement.service.impl;

import com.example.booksManagement.model.Book;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Book findById(Long id) {
        // ИЗМЕНЕНО: Логика проверки теперь здесь
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id));
    }

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public Book update(Book book) {
        // Для update мы тоже должны сначала найти сущность.
        // Метод save() в Spring Data JPA работает как "upsert", если ID существует,
        // но хорошей практикой является явная проверка.
        findById(book.getId()); // Проверяем, что книга существует, перед обновлением
        return bookRepository.save(book);
    }

    @Override
    public void deleteById(Long id) {
        // ИЗМЕНЕНО: Логика проверки теперь здесь
        findById(id); // Проверяем, что книга существует, перед удалением
        bookRepository.deleteById(id);
    }
}