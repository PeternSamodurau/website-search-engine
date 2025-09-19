package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.aop.annotation.CheckOwnership;
import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse findById(Long id);

    List<CommentResponse> findAllByNewsId(Long newsId);

    CommentResponse create(CommentRequest request);

    @CheckOwnership(entityType = "comment")
    CommentResponse update(Long id, CommentRequest request);

    @CheckOwnership(entityType = "comment")
    void deleteById(Long id);
}
