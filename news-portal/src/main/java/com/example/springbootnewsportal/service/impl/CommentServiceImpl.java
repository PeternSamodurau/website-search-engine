package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.request.CommentUpdateRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;
import com.example.springbootnewsportal.exception.DuplicateCommentException;
import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.mapper.CommentMapper;
import com.example.springbootnewsportal.model.Comment;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CommentRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import com.example.springbootnewsportal.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public CommentResponse findById(Long id) {
        log.info("Executing findById request for comment with ID: {}", id);
        return commentRepository.findById(id)
                .map(commentMapper::toResponse)
                .orElseThrow(() -> {
                    log.error("Comment not found with ID: {}", id);
                    return new ResourceNotFoundException("Comment not found with ID: " + id);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findAllByNewsId(Long newsId) {
        log.info("Executing findAllByNewsId request for news with ID: {}", newsId);
        List<CommentResponse> comments = commentRepository.findAllByNewsId(newsId).stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Found {} comments for news with ID: {}", comments.size(), newsId);
        return comments;
    }

    @Override
    public CommentResponse create(CommentRequest request) {
        log.info("Executing create request for new comment on news with ID: {}", request.getNewsId());

        if (commentRepository.existsByTextAndAuthorIdAndNewsId(request.getText(), request.getAuthorId(), request.getNewsId())) {
            log.warn("Attempted to create a duplicate comment. AuthorId: {}, NewsId: {}", request.getAuthorId(), request.getNewsId());
            throw new DuplicateCommentException("This user has already posted this exact comment on this news item.");
        }

        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> {
                    log.error("Cannot create comment. User (author) not found with ID: {}", request.getAuthorId());
                    return new ResourceNotFoundException("User not found with ID: " + request.getAuthorId());
                });
        News news = newsRepository.findById(request.getNewsId())
                .orElseThrow(() -> {
                    log.error("Cannot create comment. News not found with ID: {}", request.getNewsId());
                    return new ResourceNotFoundException("News not found with ID: " + request.getNewsId());
                });

        Comment comment = commentMapper.toComment(request);
        comment.setAuthor(author);
        comment.setNews(news);

        Comment savedComment = commentRepository.save(comment);
        log.info("Successfully created comment with ID: {}", savedComment.getId());
        return commentMapper.toResponse(savedComment);
    }

    @Override
    public CommentResponse update(Long id, CommentUpdateRequest request) {
        log.info("Executing update request for comment with ID: {}", id);
        Comment existingComment = commentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot update. Comment not found with ID: {}", id);
                    return new ResourceNotFoundException("Comment not found with ID: " + id);
                });

        commentMapper.updateCommentFromRequest(request, existingComment);

        Comment updatedComment = commentRepository.save(existingComment);
        log.info("Successfully updated comment with ID: {}", updatedComment.getId());
        return commentMapper.toResponse(updatedComment);
    }

    @Override
    public void deleteById(Long id) {
        log.info("Executing deleteById request for comment with ID: {}", id);
        if (!commentRepository.existsById(id)) {
            log.error("Cannot delete. Comment not found with ID: {}", id);
            throw new ResourceNotFoundException("Comment not found with ID: " + id);
        }
        commentRepository.deleteById(id);
        log.info("Successfully deleted comment with ID: {}", id);
    }
}
