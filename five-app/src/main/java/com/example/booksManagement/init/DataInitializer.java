package com.example.booksManagement.init;

import com.example.booksManagement.client.OpenLibraryClient;
import com.example.booksManagement.client.openlibrary.OpenLibraryBookDoc;
import com.example.booksManagement.client.openlibrary.OpenLibrarySearchResponse;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile("init")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final OpenLibraryClient openLibraryClient;

    @Override
    @Transactional
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            log.info("Database is not empty. Skipping data initialization.");
            return;
        }

        log.info("Database is empty. Initializing data by fetching books from Open Library API...");
        try {
            // --- НОВАЯ ЛОГИКА: ЗАПРОСЫ ДЛЯ РАЗНЫХ КАТЕГОРИЙ ---
            List<String> subjects = List.of("History", "Science", "Fantasy", "Biography", "Cooking");
            Set<Book> booksToSave = new HashSet<>();
            int booksPerSubject = 100;

            for (String subject : subjects) {
                log.info("Fetching {} books for subject: {}", booksPerSubject, subject);
                OpenLibrarySearchResponse response = openLibraryClient.searchBooks("subject:" + subject.toLowerCase(), booksPerSubject);

                for (OpenLibraryBookDoc doc : response.getDocs()) {
                    if (doc.getTitle() == null || doc.getAuthorName() == null || doc.getAuthorName().isEmpty()) {
                        continue;
                    }

                    // Используем название темы, по которой искали, как имя категории
                    // Это гарантирует правильное распределение
                    Category category = categoryRepository.findByName(subject)
                            .orElseGet(() -> categoryRepository.save(Category.builder().name(subject).build()));

                    Book book = Book.builder()
                            .title(doc.getTitle())
                            .author(doc.getAuthorName().get(0))
                            .category(category)
                            .build();

                    booksToSave.add(book);
                }
            }
            // --- КОНЕЦ НОВОЙ ЛОГИКИ ---

            bookRepository.saveAll(booksToSave);
            log.info("Data initialization finished. Total books in database: {}. Total categories: {}",
                    bookRepository.count(), categoryRepository.count());

        } catch (Exception e) {
            log.error("Failed to initialize data from Open Library API.", e);
        }
    }
}