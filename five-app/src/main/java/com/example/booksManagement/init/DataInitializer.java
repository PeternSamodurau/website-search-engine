package com.example.booksManagement.init;

import com.example.booksManagement.client.GoogleBooksClient;
import com.example.booksManagement.client.googlebooks.GoogleBooksSearchResponse;
import com.example.booksManagement.client.googlebooks.VolumeItem;
import com.example.booksManagement.model.Book;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.BookRepository;
import com.example.booksManagement.repository.CategoryRepository;
import com.example.booksManagement.service.BookService;
import com.example.booksManagement.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Profile("init")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookService bookService;
    private final CategoryService categoryService;
    private final GoogleBooksClient googleBooksClient;

    public DataInitializer(BookRepository bookRepository, CategoryRepository categoryRepository, BookService bookService, CategoryService categoryService, GoogleBooksClient googleBooksClient) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
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

        logger.info("Database is empty. Initializing data by fetching books from Google Books API...");

        final int booksPerQuery = 40;
        final int numberOfQueries = 13; // 13 * 40 = ~520 books

        logger.info("Attempting to fetch books in {} batches...", numberOfQueries);

        for (int i = 0; i < numberOfQueries; i++) {
            try {
                // ИСПРАВЛЕНО: Используем простой и рабочий поисковый запрос "a".
                // Это даст нам гарантированный и разнообразный результат.
                GoogleBooksSearchResponse response = googleBooksClient.searchBooks("a", booksPerQuery);

                if (response == null || response.getItems() == null) {
                    logger.warn("Received no books on attempt {}. Skipping batch.", i + 1);
                    continue;
                }

                for (VolumeItem item : response.getItems()) {
                    if (item.getVolumeInfo() == null || item.getVolumeInfo().getTitle() == null) {
                        continue;
                    }

                    // Проверяем, есть ли у книги категория. Если нет - пропускаем.
                    List<String> bookCategories = item.getVolumeInfo().getCategories();
                    if (bookCategories == null || bookCategories.isEmpty()) {
                        continue;
                    }
                    String categoryName = bookCategories.get(0);
                    if (categoryName == null || categoryName.trim().isEmpty()) {
                        continue;
                    }
                    String trimmedCategoryName = categoryName.trim();

                    // Ищем категорию в базе данных.
                    Optional<Category> existingCategory = categoryRepository.findByName(trimmedCategoryName);

                    Category categoryToSave;
                    if (existingCategory.isPresent()) {
                        // Если нашли - используем ее.
                        categoryToSave = existingCategory.get();
                    } else {
                        // Если не нашли - создаем новую и сохраняем ее.
                        logger.info("Discovered and creating new category: '{}'", trimmedCategoryName);
                        Category newCategory = new Category();
                        newCategory.setName(trimmedCategoryName);
                        categoryToSave = categoryService.save(newCategory);
                    }

                    // Сохраняем книгу в базу.
                    Book book = new Book();
                    book.setTitle(item.getVolumeInfo().getTitle());
                    book.setAuthor(item.getVolumeInfo().getAuthors() != null ? String.join(", ", item.getVolumeInfo().getAuthors()) : "Unknown Author");
                    book.setCategory(categoryToSave);
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