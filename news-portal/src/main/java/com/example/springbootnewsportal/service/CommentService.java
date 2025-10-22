package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.request.CommentUpdateRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;

import java.security.Principal;
import java.util.List;

public interface CommentService {

    CommentResponse findById(Long id);

    List<CommentResponse> findAllByNewsId(Long newsId);

    CommentResponse create(Long newsId, CommentRequest request, Principal principal);

    CommentResponse update(Long id, CommentUpdateRequest request);

    void deleteById(Long id);

    boolean isCommentAuthor(Long commentId, String username);
}
