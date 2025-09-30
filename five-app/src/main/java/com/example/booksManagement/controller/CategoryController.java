package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UserCategoryRequest; // ИСПРАВЛЕНО
import com.example.booksManagement.dto.response.CategoryResponse;
import com.example.booksManagement.mappers.CategoryMapper;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> responses = categoryService.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        // Теперь просто вызываем сервис
        Category category = categoryService.findById(id);
        return ResponseEntity.ok(categoryMapper.toResponse(category));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody UserCategoryRequest request) { // ИСПРАВЛЕНО
        Category newCategory = categoryMapper.toEntity(request);
        Category savedCategory = categoryService.save(newCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryMapper.toResponse(savedCategory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody UserCategoryRequest request) { // ИСПРАВЛЕНО
        // Сначала находим сущность
        Category category = categoryService.findById(id);
        // Затем обновляем ее данными из запроса
        categoryMapper.updateEntity(request, category);
        // И сохраняем обновленную сущность
        Category updatedCategory = categoryService.update(category);
        return ResponseEntity.ok(categoryMapper.toResponse(updatedCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        // Теперь просто вызываем сервис
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}