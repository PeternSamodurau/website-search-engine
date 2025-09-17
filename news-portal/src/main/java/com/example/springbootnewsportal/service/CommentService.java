package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;

public interface CommentService {

    CommentResponse findById(Long id);

    CommentResponse create(Long newsId, CommentRequest request);

    CommentResponse update(Long id, CommentRequest request);

    void deleteById(Long id);
}
