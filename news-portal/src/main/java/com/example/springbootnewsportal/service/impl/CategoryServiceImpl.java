package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.dto.CategoryResponse;
import com.example.springbootnewsportal.exception.EntityNotFoundException;
import com.example.springbootnewsportal.mapper.CategoryMapper;
import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public Page<CategoryResponse> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(categoryMapper::categoryToResponse);
    }

    @Override
    public CategoryResponse findById(Long id) {
        return categoryMapper.categoryToResponse(getCategoryById(id));
    }

    @Override
    public CategoryResponse save(UpsertCategoryRequest request) {
        Category newCategory = categoryMapper.requestToCategory(request);
        return categoryMapper.categoryToResponse(categoryRepository.save(newCategory));
    }

    @Override
    public CategoryResponse update(Long id, UpsertCategoryRequest request) {
        Category existingCategory = getCategoryById(id);
        Category updatedCategory = categoryMapper.requestToCategory(id, request);
        updatedCategory.setNewsList(existingCategory.getNewsList()); // Preserve relationships
        return categoryMapper.categoryToResponse(categoryRepository.save(updatedCategory));
    }

    @Override
    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }

    private Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + id + " not found"));
    }
}
