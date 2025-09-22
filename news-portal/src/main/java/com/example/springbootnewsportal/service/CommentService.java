package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.aop.annotation.CheckOwnership;
import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.request.CommentUpdateRequest; // <--- ИЗМЕНЕНИЕ
import com.example.springbootnewsportal.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse findById(Long id);

    List<CommentResponse> findAllByNewsId(Long newsId);

    CommentResponse create(CommentRequest request);

    // === БЛОК ИЗМЕНЕНИЙ НАЧАЛО ===
    @CheckOwnership(entityType = "comment")
    CommentResponse update(Long id, CommentUpdateRequest request); // <--- ИЗМЕНЕНИЕ
    // === БЛОК ИЗМЕНЕНИЙ КОНЕЦ ===

    @CheckOwnership(entityType = "comment")
    void deleteById(Long id);
}
