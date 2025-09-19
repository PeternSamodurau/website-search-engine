package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import java.util.List;

public interface NewsService {

    List<NewsResponse> findAll(Long authorId, Long categoryId);

    NewsResponse findById(Long id);

    NewsResponse create(NewsRequest request);

    NewsResponse update(Long id, NewsRequest request);

    void deleteById(Long id);
}
