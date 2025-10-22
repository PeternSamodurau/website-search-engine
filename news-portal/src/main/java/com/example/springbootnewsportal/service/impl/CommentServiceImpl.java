package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.request.CommentUpdateRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;
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

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentResponse create(Long newsId, CommentRequest request, Principal principal) {
        String username = principal.getName();
        log.info("Создание комментария для новости с ID: {} от пользователя: {}", newsId, username);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("Новость с ID " + newsId + " не найдена."));

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с именем " + username + " не найден."));

        // ИСПОЛЬЗУЕМ МАППЕР для создания Comment из DTO
        Comment comment = commentMapper.toComment(request);
        comment.setNews(news);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        log.info("Комментарий с ID: {} успешно создан.", savedComment.getId());
        return commentMapper.toResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findAllByNewsId(Long newsId) {
        log.info("Поиск всех комментариев для новости с ID: {}", newsId);
        List<Comment> comments = commentRepository.findAllByNewsId(newsId);
        log.info("Найдено {} комментариев для новости с ID: {}", comments.size(), newsId);
        // ИСПОЛЬЗУЕМ МАППЕР для конвертации всего списка
        return commentMapper.toResponseList(comments);
    }


    @Override
    @Transactional(readOnly = true)
    public CommentResponse findById(Long id) {
        log.info("Поиск комментария по ID: {}", id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Комментарий с ID " + id + " не найден."));
        log.info("Комментарий с ID: {} найден.", id);
        return commentMapper.toResponse(comment);
    }


    @Override
    @Transactional
    public CommentResponse update(Long id, CommentUpdateRequest request) {
        log.info("Обновление комментария с ID: {}", id);
        Comment existingComment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Комментарий с ID " + id + " не найден."));

        // ИСПОЛЬЗУЕМ МАППЕР для обновления сущности из DTO
        commentMapper.updateCommentFromRequest(request, existingComment);
        existingComment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(existingComment);
        log.info("Комментарий с ID: {} успешно обновлен.", updatedComment.getId());
        return commentMapper.toResponse(updatedComment);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("Удаление комментария с ID: {}", id);
        if (!commentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Комментарий с ID " + id + " не найден.");
        }
        commentRepository.deleteById(id);
        log.info("Комментарий с ID: {} успешно удален.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCommentAuthor(Long commentId, String username) {
        log.debug("Проверка авторства комментария ID: {} для пользователя: {}", commentId, username);
        return commentRepository.findById(commentId)
                .map(comment -> comment.getAuthor().getUsername().equals(username))
                .orElse(false);
    }
}