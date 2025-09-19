package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.service.CategoryService;
import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import com.example.springbootnewsportal.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        log.info("Executing findAll categories request");
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        log.info("Executing findById request for category with ID: {}", id);
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", id);
                    return new ResourceNotFoundException("Category not found with ID: " + id);
                });
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        log.info("Executing create request for new category with name: '{}'", request.getName());
        Category category = categoryMapper.toCategory(request);
        Category savedCategory = categoryRepository.save(category);
        log.info("Successfully created category with ID: {}", savedCategory.getId());
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        log.info("Executing update request for category with ID: {}", id);
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot update. Category not found with ID: {}", id);
                    return new ResourceNotFoundException("Category not found with ID: " + id);
                });

        categoryMapper.updateCategoryFromRequest(request, existingCategory);
        Category updatedCategory = categoryRepository.save(existingCategory);
        log.info("Successfully updated category with ID: {}", updatedCategory.getId());
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public void deleteById(Long id) {
        log.info("Executing deleteById request for category with ID: {}", id);
        if (!categoryRepository.existsById(id)) {
            log.error("Cannot delete. Category not found with ID: {}", id);
            throw new ResourceNotFoundException("Category not found with ID: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Successfully deleted category with ID: {}", id);
    }
}
