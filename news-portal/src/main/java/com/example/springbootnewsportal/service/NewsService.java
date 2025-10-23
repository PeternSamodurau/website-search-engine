package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.request.NewsUpdateRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;

public interface NewsService {

    Page<NewsResponse> findAll(Long categoryId, Pageable pageable);

    NewsResponse findById(Long id);

    NewsResponse create(NewsRequest request, Principal principal);

    NewsResponse update(Long id, NewsUpdateRequest request);

    void deleteById(Long id);

    boolean isNewsAuthor(Long newsId, String username);
}
