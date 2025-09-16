package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    Page<CategoryResponse> findAll(Pageable pageable);

    CategoryResponse findById(Long id);

    CategoryResponse save(UpsertCategoryRequest request);

    CategoryResponse update(Long id, UpsertCategoryRequest request);

    void deleteById(Long id);
}
