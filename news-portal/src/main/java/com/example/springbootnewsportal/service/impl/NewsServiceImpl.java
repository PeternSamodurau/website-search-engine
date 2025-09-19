package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.aop.CheckEntityOwnership;
import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import com.example.springbootnewsportal.service.NewsService;
import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NewsMapper newsMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NewsResponse> findAll(Long authorId, Long categoryId) {
        log.info("Executing findAll news request with authorId: {} and categoryId: {}", authorId, categoryId);
        Specification<News> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (authorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("author").get("id"), authorId));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<News> newsList = newsRepository.findAll(spec);
        log.info("Found {} news items matching criteria", newsList.size());
        return newsList.stream()
                .map(newsMapper::toNewsResponseForList)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NewsResponse findById(Long id) {
        log.info("Executing findById request for news with ID: {}", id);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("News not found with ID: {}", id);
                    return new ResourceNotFoundException("News not found with ID: " + id);
                });
        log.info("Successfully found news with ID: {}", id);
        return newsMapper.toNewsResponseWithComments(news);
    }

    @Override
    public NewsResponse create(NewsRequest request) {
        log.info("Executing create request for new news with title: '{}'", request.getTitle());
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> {
                    log.error("Cannot create news. User (author) not found with ID: {}", request.getAuthorId());
                    return new ResourceNotFoundException("User not found with ID: " + request.getAuthorId());
                });
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Cannot create news. Category not found with ID: {}", request.getCategoryId());
                    return new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId());
                });

        News news = newsMapper.toNews(request);
        news.setAuthor(author);
        news.setCategory(category);

        News savedNews = newsRepository.save(news);
        log.info("Successfully created news with ID: {}", savedNews.getId());
        return newsMapper.toNewsResponseWithComments(savedNews);
    }

    @Override
    @CheckEntityOwnership
    public NewsResponse update(Long id, NewsRequest request) {
        log.info("Executing update request for news with ID: {}", id);
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot update. News not found with ID: {}", id);
                    return new ResourceNotFoundException("News not found with ID: " + id);
                });

        if (request.getCategoryId() != null) {
            log.info("Updating category for news with ID: {}", id);
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> {
                        log.error("Cannot update news. New category not found with ID: {}", request.getCategoryId());
                        return new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId());
                    });
            existingNews.setCategory(newCategory);
        }

        newsMapper.updateNewsFromRequest(request, existingNews);

        News updatedNews = newsRepository.save(existingNews);
        log.info("Successfully updated news with ID: {}", updatedNews.getId());
        return newsMapper.toNewsResponseWithComments(updatedNews);
    }

    @Override
    @CheckEntityOwnership
    public void deleteById(Long id) {
        log.info("Executing deleteById request for news with ID: {}", id);
        if (!newsRepository.existsById(id)) {
            log.error("Cannot delete. News not found with ID: {}", id);
            throw new ResourceNotFoundException("News not found with ID: " + id);
        }
        newsRepository.deleteById(id);
        log.info("Successfully deleted news with ID: {}", id);
    }
}