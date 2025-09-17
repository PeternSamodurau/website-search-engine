package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    Page<CategoryResponse> findAll(Pageable pageable);

    CategoryResponse findById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void deleteById(Long id);
}
