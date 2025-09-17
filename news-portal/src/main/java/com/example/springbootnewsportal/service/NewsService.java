package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsService {

    Page<NewsResponse> findAll(Long authorId, Long categoryId, Pageable pageable);

    NewsResponse findById(Long id);

    NewsResponse create(NewsRequest request);

    NewsResponse update(Long id, NewsRequest request);

    void deleteById(Long id);
}
