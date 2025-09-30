package com.example.booksManagement.service.impl;

import com.example.booksManagement.model.Category;
import com.example.booksManagement.repository.CategoryRepository;
import com.example.booksManagement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category findById(Long id) {
        // ИЗМЕНЕНО: Логика проверки теперь здесь
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));
    }

    @Override
    public Category findByName(String name) {
        // ИЗМЕНЕНО: Логика проверки теперь здесь
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with name: " + name));
    }

    @Override
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Category update(Category category) {
        // Проверяем, что категория существует, перед обновлением
        findById(category.getId());
        return categoryRepository.save(category);
    }

    @Override
    public void deleteById(Long id) {
        // ИЗМЕНЕНО: Логика проверки теперь здесь
        findById(id); // Проверяем, что категория существует, перед удалением
        categoryRepository.deleteById(id);
    }
}