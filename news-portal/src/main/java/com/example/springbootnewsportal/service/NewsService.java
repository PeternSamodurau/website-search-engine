package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.aop.annotation.CheckOwnership;
import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.request.NewsUpdateRequest; // <--- ИЗМЕНЕНИЕ
import com.example.springbootnewsportal.dto.response.NewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsService {

    Page<NewsResponse> findAll(Long authorId, Long categoryId, Pageable pageable);

    NewsResponse findById(Long id);

    NewsResponse create(NewsRequest request);

    // === БЛОК ИЗМЕНЕНИЙ НАЧАЛО ===
    @CheckOwnership(entityType = "news")
    NewsResponse update(Long id, NewsUpdateRequest request); // <--- ИЗМЕНЕНИЕ
    // === БЛОК ИЗМЕНЕНИЙ КОНЕЦ ===

    @CheckOwnership(entityType = "news")
    void deleteById(Long id);
}
