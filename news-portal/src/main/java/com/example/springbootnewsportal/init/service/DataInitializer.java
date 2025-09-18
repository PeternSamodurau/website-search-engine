package com.example.springbootnewsportal.init.service;

import com.example.springbootnewsportal.init.dto.NewsApiResponse;
import com.example.springbootnewsportal.init.dto.NewsDto;
import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("init")
public class DataInitializer implements CommandLineRunner {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NewsRepository newsRepository;

    @Value("${news.api.key}")
    private String apiKey;

    @Value("${news.api.query}")
    private String newsQuery;

    @Override
    public void run(String... args) {
        log.info("Profile 'init' is active. Starting data initialization for query: '{}'", newsQuery);

        try {
            String fromDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String newsApiUrl = String.format(
                    "https://newsapi.org/v2/everything?q=%s&from=%s&sortBy=publishedAt&apiKey=%s",
                    newsQuery,
                    fromDate,
                    apiKey
            );

            log.info("Fetching news from URL: {}", newsApiUrl);
            NewsApiResponse response = restTemplate.getForObject(newsApiUrl, NewsApiResponse.class);

            if (response == null || response.getArticles() == null || response.getArticles().isEmpty()) {
                log.warn("Failed to fetch news from the API or the article list is empty.");
                return;
            }

            List<NewsDto> articlesToSave = response.getArticles().stream().limit(10).toList();
            log.info("Received {} articles to save.", articlesToSave.size());

            String categoryNameFormatted = newsQuery.substring(0, 1).toUpperCase() + newsQuery.substring(1).toLowerCase();
            log.info("Formatted category name: '{}'", categoryNameFormatted);

            Optional<Category> existingCategory = categoryRepository.findByCategoryName(categoryNameFormatted);

            Category category;
            if (existingCategory.isPresent()) {

                category = existingCategory.get();
                log.info("Category '{}' already exists. Using it.", category.getCategoryName());
            } else {

                log.info("Category '{}' not found. Creating a new one.", categoryNameFormatted);
                Category newCategoryToSave = Category.builder()
                        .categoryName(categoryNameFormatted)
                        .build();
                category = categoryRepository.save(newCategoryToSave);
                log.info("Successfully saved new category: {}", category);
            }

            for (NewsDto newsDto : articlesToSave) {
                if (newsDto.getAuthor() == null || newsDto.getContent() == null) {
                    log.warn("Skipping article with no author or content. Title: {}", newsDto.getTitle());
                    continue;
                }

                User author = userRepository.findByUsername(newsDto.getAuthor())
                        .orElseGet(() -> {
                            User newUser = User.builder()
                                    .username(newsDto.getAuthor())
                                    .email(newsDto.getAuthor().replaceAll("\s+", "_") + "@example.com")
                                    .password(UUID.randomUUID().toString())
                                    .build();
                            return userRepository.save(newUser);
                        });

                News news = News.builder()
                        .sourceName(newsDto.getSourceName())
                        .title(newsDto.getTitle())
                        .description(newsDto.getDescription())
                        .content(newsDto.getContent())
                        .publishedAt(newsDto.getPublishedAt())
                        .url(newsDto.getUrl())
                        .imageUrl(newsDto.getUrlToImage())
                        .author(author)
                        .category(category)
                        .build();

                newsRepository.save(news);
            }

            log.info("Data initialization completed successfully. Saved {} news articles for category '{}'.", articlesToSave.size(), category.getCategoryName());

        } catch (Exception e) {
            log.error("An error occurred during data initialization: {}", e.getMessage(), e);
        }
    }
}
