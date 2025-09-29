package com.example.booksManagement.controller;

import com.example.booksManagement.dto.request.UpsertCategoryRequest;
import com.example.booksManagement.dto.response.CategoryResponse;
import com.example.booksManagement.model.Category;
import com.example.booksManagement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // Простой маппер для преобразования Entity в Response DTO
    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        return response;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> responses = categoryService.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));
        return ResponseEntity.ok(toResponse(category));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody UpsertCategoryRequest request) {
        Category newCategory = new Category();
        newCategory.setName(request.getName());
        Category savedCategory = categoryService.save(newCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(savedCategory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody UpsertCategoryRequest request) {
        Category category = categoryService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));
        category.setName(request.getName());
        Category updatedCategory = categoryService.update(category);
        return ResponseEntity.ok(toResponse(updatedCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (categoryService.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id);
        }
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}