package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.service.CategoryService;
import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import com.example.springbootnewsportal.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(categoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.toCategory(request);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        categoryMapper.updateCategoryFromRequest(request, existingCategory);

        Category updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public void deleteById(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with ID: " + id);
        }
        categoryRepository.deleteById(id);
    }
}