package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> findAll();

    CategoryResponse findById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void deleteById(Long id);
}
