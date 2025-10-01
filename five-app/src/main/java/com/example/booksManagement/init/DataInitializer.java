package com.example.booksManagement.init;

import com.example.booksManagement.client.GoogleBooksClient;
import com.example.booksManagement.client.googlebooks.GoogleBooksSearchResponse;
import com.example.booksManagement.client.googlebooks.VolumeItem;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.repository.CategoryRepository;
import com.example.booksManagement.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("init")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookService bookService;
    private final GoogleBooksClient googleBooksClient;

    // Конструктор теперь не требует CategoryService
    public DataInitializer(BookRepository bookRepository, CategoryRepository categoryRepository, BookService bookService, GoogleBooksClient googleBooksClient) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.bookService = bookService;
        this.googleBooksClient = googleBooksClient;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            logger.info("Database is not empty. Skipping data initialization.");
            return;
        }

        logger.info("Database is empty. Initializing data by fetching books from Google Books API...");

        final int booksPerQuery = 40;
        final int numberOfQueries = 13;

        logger.info("Attempting to fetch books in {} batches...", numberOfQueries);

        for (int i = 0; i < numberOfQueries; i++) {
            try {
                GoogleBooksSearchResponse response = googleBooksClient.searchBooks("a", booksPerQuery);

                if (response == null || response.getItems() == null) {
                    logger.warn("Received no books on attempt {}. Skipping batch.", i + 1);
                    continue;
                }

                for (VolumeItem item : response.getItems()) {
                    if (item.getVolumeInfo() == null || item.getVolumeInfo().getTitle() == null) {
                        continue;
                    }

                    List<String> bookCategories = item.getVolumeInfo().getCategories();
                    if (bookCategories == null || bookCategories.isEmpty() || bookCategories.get(0) == null || bookCategories.get(0).isBlank()) {
                        continue;
                    }
                    String categoryName = bookCategories.get(0).trim();

                    // 1. Создаем временный объект Category только с именем.
                    Category transientCategory = new Category();
                    transientCategory.setName(categoryName);

                    // 2. Создаем книгу и передаем в нее временную категорию.
                    Book book = new Book();
                    book.setTitle(item.getVolumeInfo().getTitle());
                    book.setAuthor(item.getVolumeInfo().getAuthors() != null ? String.join(", ", item.getVolumeInfo().getAuthors()) : "Unknown Author");
                    book.setCategory(transientCategory);

                    // 3. Доверяем BookService всю работу по поиску или созданию реальной категории в БД.
                    bookService.save(book);
                }
            } catch (Exception e) {
                logger.error("An error occurred during batch {}. Error: {}. Skipping batch.", i + 1, e.getMessage(), e);
            }
        }

        logger.info("Data initialization finished. Total books in database: {}. Total categories: {}",
                bookRepository.count(), categoryRepository.count());
    }
}