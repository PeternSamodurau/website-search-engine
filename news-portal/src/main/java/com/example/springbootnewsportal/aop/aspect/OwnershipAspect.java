
package com.example.springbootnewsportal.aop.aspect;

import com.example.springbootnewsportal.aop.annotation.CheckOwnership;
import com.example.springbootnewsportal.exception.AccessDeniedException;
import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.Comment;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CommentRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OwnershipAspect {

    private final NewsRepository newsRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Pointcut("@annotation(checkOwnership) && args(entityId, ..)")
    public void checkOwnershipPointcut(CheckOwnership checkOwnership, Long entityId) {}

    @Before("checkOwnershipPointcut(checkOwnership, entityId)")
    public void checkOwnership(CheckOwnership checkOwnership, Long entityId) {
        log.info("====== AOP OWNERSHIP CHECK TRIGGERED (TYPE-SAFE) ======");

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof String username)) {
            log.error("Principal is not a String. Unexpected principal type: {}", principal.getClass().getName());
            throw new AccessDeniedException("You must be logged in to perform this action.");
        }

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User with username '{}' not found in database.", username);
                    return new ResourceNotFoundException("User not found: " + username);
                });

        log.info("Checking ownership for entityType: '{}', entityId: '{}'", checkOwnership.entityType(), entityId);
        log.info("Current user: '{}' (ID: {})", currentUser.getUsername(), currentUser.getId());

        boolean isOwner = switch (checkOwnership.entityType()) {
            case "news" -> newsRepository.findById(entityId)
                    .map(News::getAuthor)
                    .map(author -> Objects.equals(author.getId(), currentUser.getId()))
                    .orElse(false);
            case "comment" -> commentRepository.findById(entityId)
                    .map(Comment::getAuthor)
                    .map(author -> Objects.equals(author.getId(), currentUser.getId()))
                    .orElse(false);
            default -> false;
        };

        log.info("Ownership check result: {}", isOwner);

        if (!isOwner) {
            log.error("Ownership check FAILED for user '{}' on entity '{}' with id '{}'",
                    currentUser.getUsername(), checkOwnership.entityType(), entityId);
            throw new AccessDeniedException("You do not have permission to modify or delete this resource.");
        }

        log.info("====== AOP OWNERSHIP CHECK PASSED ======");
    }
}
