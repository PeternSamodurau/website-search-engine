package com.example.springbootnewsportal.init.service;

import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("init")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NewsRepository newsRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("Data initialization started...");
            if (userRepository.count() == 0) {
                initializeUsers();
            }
            if (categoryRepository.count() == 0) {
                initializeCategories();
            }
            if (newsRepository.count() == 0) {
                initializeNews();
            }
            log.info("Data initialization finished.");
        } catch (Exception e) {
            log.error("Error during data initialization", e);
        }
    }

    private void initializeUsers() throws Exception {
        log.info("Initializing users...");
        File file = new ClassPathResource("data/users.json").getFile();
        List<User> users = objectMapper.readValue(file, new TypeReference<>() {});
        users.forEach(user -> user.setPassword(passwordEncoder.encode(user.getPassword())));
        userRepository.saveAll(users);
    }

    private void initializeCategories() throws Exception {
        log.info("Initializing categories...");
        File file = new ClassPathResource("data/categories.json").getFile();
        List<Category> categories = objectMapper.readValue(file, new TypeReference<>() {});
        categoryRepository.saveAll(categories);
    }

    private void initializeNews() throws Exception {
        log.info("Initializing news...");
        File file = new ClassPathResource("data/news.json").getFile();
        // ИЗМЕНЕНИЕ: Используем Map вместо NewsDto
        List<Map<String, String>> newsData = objectMapper.readValue(file, new TypeReference<>() {});

        List<User> users = userRepository.findAll();
        List<Category> categories = categoryRepository.findAll();
        Random random = new Random();

        if (users.isEmpty() || categories.isEmpty()) {
            log.warn("Cannot initialize news because there are no users or categories.");
            return;
        }

        List<News> newsList = new ArrayList<>();
        for (Map<String, String> newsMap : newsData) { // <-- ИЗМЕНЕНИЕ
            News news = News.builder()
                    .title(newsMap.get("title")) // <-- ИЗМЕНЕНИЕ
                    .content(newsMap.get("content")) // <-- ИЗМЕНЕНИЕ
                    .author(users.get(random.nextInt(users.size())))
                    .category(categories.get(random.nextInt(categories.size())))
                    .build();
            newsList.add(news);
        }
        newsRepository.saveAll(newsList);
    }
}