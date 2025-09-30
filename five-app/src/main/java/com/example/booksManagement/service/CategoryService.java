package com.example.booksManagement.service;

import com.example.booksManagement.model.Category;
import java.util.List;

public interface CategoryService {
    List<Category> findAll();

    /**
     * Находит категорию по ID.
     * @param id ID категории.
     * @return Найденную сущность Category.
     * @throws org.springframework.web.server.ResponseStatusException если категория не найдена.
     */
    Category findById(Long id);

    /**
     * Находит категорию по имени.
     * @param name Имя категории.
     * @return Найденную сущность Category.
     * @throws org.springframework.web.server.ResponseStatusException если категория не найдена.
     */
    Category findByName(String name);

    Category save(Category category);
    Category update(Category category);

    /**
     * Удаляет категорию по ID.
     * @param id ID категории.
     * @throws org.springframework.web.server.ResponseStatusException если категория не найдена.
     */
    void deleteById(Long id);
}