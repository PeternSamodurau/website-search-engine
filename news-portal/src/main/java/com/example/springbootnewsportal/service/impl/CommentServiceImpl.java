package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.Comment;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CommentRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import com.example.springbootnewsportal.service.CommentService;
import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;
import com.example.springbootnewsportal.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public CommentResponse findById(Long id) {
        return commentRepository.findById(id)
                .map(commentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + id));
    }

    @Override
    public CommentResponse create(Long newsId, CommentRequest request) {
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getAuthorId()));
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with ID: " + newsId));

        Comment comment = commentMapper.toComment(request);
        comment.setAuthor(author);
        comment.setNews(news);

        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toResponse(savedComment);
    }

    @Override
    public CommentResponse update(Long id, CommentRequest request) {
        Comment existingComment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + id));

        commentMapper.updateCommentFromRequest(request, existingComment);

        Comment updatedComment = commentRepository.save(existingComment);
        return commentMapper.toResponse(updatedComment);
    }

    @Override
    public void deleteById(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Comment not found with ID: " + id);
        }
        commentRepository.deleteById(id);
    }
}