package com.example.booksManagement.init;

import com.example.booksManagement.client.GoogleBooksClient;
import com.example.booksManagement.client.googlebooks.GoogleBooksSearchResponse;
import com.example.booksManagement.client.googlebooks.VolumeItem;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.service.BookService;
import com.example.booksManagement.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Profile("init")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final int BOOKS_PER_CATEGORY = 10;

    private final BookRepository bookRepository;
    private final BookService bookService;
    private final CategoryService categoryService;
    private final GoogleBooksClient googleBooksClient;

    public DataInitializer(BookRepository bookRepository, BookService bookService, CategoryService categoryService, GoogleBooksClient googleBooksClient) {
        this.bookRepository = bookRepository;
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.googleBooksClient = googleBooksClient;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            logger.info("Database is not empty. Skipping data initialization.");
            return;
        }

        logger.info("Database is empty. Starting data initialization...");

        List<String> categories = Arrays.asList(
                "Fantasy", "Philosophy", "Thriller", "Business", "Travel", "Mystery",
                "Technology", "History", "Science", "Cooking", "Art", "Fiction",
                "Biography", "Romance", "Music"
        );

        logger.info("Processing {} predefined categories.", categories.size());

        for (String categoryName : categories) {
            try {
                // ПРАВИЛЬНАЯ ЛОГИКА: Найти или создать категорию
                Optional<Category> existingCategory = categoryService.findByName(categoryName);
                Category category = existingCategory.orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(categoryName);
                    return categoryService.save(newCategory);
                });

                logger.info("Fetching up to {} books for category: '{}'", BOOKS_PER_CATEGORY, categoryName);

                GoogleBooksSearchResponse response = googleBooksClient.searchBooks(
                        "subject:" + categoryName,
                        BOOKS_PER_CATEGORY
                );

                if (response == null || response.getItems() == null) {
                    logger.warn("No books found for category '{}'. Skipping.", categoryName);
                    continue;
                }

                for (VolumeItem item : response.getItems()) {
                    if (item.getVolumeInfo() == null || item.getVolumeInfo().getTitle() == null) {
                        continue;
                    }

                    Book book = new Book();
                    book.setTitle(item.getVolumeInfo().getTitle());

                    if (item.getVolumeInfo().getAuthors() != null) {
                        book.setAuthor(String.join(", ", item.getVolumeInfo().getAuthors()));
                    } else {
                        book.setAuthor("Unknown Author");
                    }

                    book.setCategory(category);
                    bookService.save(book);
                }
            } catch (Exception e) {
                logger.warn("Failed to process category '{}'. Error: {}. Skipping.", categoryName, e.getMessage());
            }
        }

        logger.info("Data initialization finished. Total books in database: {}", bookRepository.count());
    }
}
