package com.example.booksManagement.service;

import com.example.booksManagement.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> findAll();
    Optional<Category> findById(Long id);
    Optional<Category> findByName(String name);
    Category save(Category category);
    Category update(Category category);
    void deleteById(Long id);
}