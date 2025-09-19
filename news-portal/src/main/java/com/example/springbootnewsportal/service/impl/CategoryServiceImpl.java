package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import com.example.springbootnewsportal.exception.EntityExistsException;
import com.example.springbootnewsportal.exception.EntityNotFoundException;
import com.example.springbootnewsportal.mapper.CategoryMapper;
import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        log.info("Executing findAll request for categories");
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        log.info("Executing findById request for category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + id + " not found"));
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        log.info("Executing create request for new category with name: '{}'", request.getCategoryName());
        if (categoryRepository.findByCategoryName(request.getCategoryName()).isPresent()) {
            throw new EntityExistsException("Category with name '" + request.getCategoryName() + "' already exists");
        }
        Category category = categoryMapper.toCategory(request);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        log.info("Executing update request for category with ID: {}", id);
        Optional<Category> existingByName = categoryRepository.findByCategoryName(request.getCategoryName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new EntityExistsException("Another category with name '" + request.getCategoryName() + "' already exists");
        }
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + id + " not found"));
        categoryMapper.updateCategoryFromRequest(request, existingCategory);
        Category updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("Executing delete request for category with ID: {}", id);
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Category with ID " + id + " not found");
        }
        categoryRepository.deleteById(id);
    }
}
