package com.example.springbootnewsportal.aop;

import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.Comment;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CommentRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipCheckAspect {

    private final NewsRepository newsRepository;
    private final CommentRepository commentRepository; // ВНЕДРЯЕМ РЕПОЗИТОРИЙ КОММЕНТАРИЕВ
    private final UserRepository userRepository;

    @Before("@annotation(com.example.springbootnewsportal.aop.CheckEntityOwnership) && args(id, ..)")
    public void checkOwnership(JoinPoint joinPoint, Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));

        String targetClassName = joinPoint.getTarget().getClass().getSimpleName();

        // ДОБАВЛЯЕМ ЛОГИКУ ПРОВЕРКИ
        if (targetClassName.equals("NewsServiceImpl")) {
            News news = newsRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("News not found with ID: " + id));
            if (!news.getAuthor().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("User does not have permission to modify this resource");
            }
        } else if (targetClassName.equals("CommentServiceImpl")) {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + id));
            if (!comment.getAuthor().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("User does not have permission to modify this resource");
            }
        } else {
            // На случай, если аннотацию повесят на другой сервис
            throw new UnsupportedOperationException("Ownership check not implemented for " + targetClassName);
        }
    }
}
