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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NewsMapper newsMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NewsResponse> findAll(Long authorId, Long categoryId, Pageable pageable) {
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

        return newsRepository.findAll(spec, pageable)
                .map(newsMapper::toNewsResponseForList);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsResponse findById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with ID: " + id));
        return newsMapper.toNewsResponseWithComments(news);
    }

    @Override
    public NewsResponse create(NewsRequest request) {
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getAuthorId()));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        News news = newsMapper.toNews(request);
        news.setAuthor(author);
        news.setCategory(category);

        News savedNews = newsRepository.save(news);
        return newsMapper.toNewsResponseWithComments(savedNews);
    }

    @Override
    @CheckEntityOwnership
    public NewsResponse update(Long id, NewsRequest request) {
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with ID: " + id));

        if (request.getCategoryId() != null) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));
            existingNews.setCategory(newCategory);
        }

        newsMapper.updateNewsFromRequest(request, existingNews);

        News updatedNews = newsRepository.save(existingNews);
        return newsMapper.toNewsResponseWithComments(updatedNews);
    }

    @Override
    @CheckEntityOwnership
    public void deleteById(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new ResourceNotFoundException("News not found with ID: " + id);
        }
        newsRepository.deleteById(id);
    }
}
