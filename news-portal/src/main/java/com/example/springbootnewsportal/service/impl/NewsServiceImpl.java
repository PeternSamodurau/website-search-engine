package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.request.NewsUpdateRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.exception.DuplicateNewsException;
import com.example.springbootnewsportal.exception.EntityNotFoundException;
import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.mapper.NewsMapper;
import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.model.News;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.CategoryRepository;
import com.example.springbootnewsportal.repository.NewsRepository;
import com.example.springbootnewsportal.repository.UserRepository;
import com.example.springbootnewsportal.repository.specification.NewsSpecification;
import com.example.springbootnewsportal.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<NewsResponse> findAll(Long authorId, Long categoryId, Pageable pageable) {
        log.info("Finding all news with authorId: {}, categoryId: {}, pageable: {}", authorId, categoryId, pageable);
        Specification<News> spec = NewsSpecification.filterBy(authorId, categoryId);
        Page<News> newsPage = newsRepository.findAll(spec, pageable);
        return newsPage.map(newsMapper::toNewsResponseForList);
    }

    @Override
    public NewsResponse findById(Long id) {
        log.info("Finding news by id: {}", id);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        return newsMapper.toNewsResponseWithComments(news);
    }

    @Override
    @Transactional
    public NewsResponse create(NewsRequest request) {
        log.info("Creating new news with title: '{}', authorId: {}, categoryId: {}",
                request.getTitle(), request.getAuthorId(), request.getCategoryId());

        // === БЛОК ИЗМЕНЕНИЙ НАЧАЛО ===
        if (newsRepository.existsByTitle(request.getTitle())) {
            log.warn("Attempted to create a news with a duplicate title: '{}'", request.getTitle());
            throw new DuplicateNewsException("A news item with the same title already exists.");
        }

        if (newsRepository.existsByText(request.getText())) {
            log.warn("Attempted to create a news with a duplicate text.");
            throw new DuplicateNewsException("A news item with the same text already exists.");
        }
        // === БЛОК ИЗМЕНЕНИЙ КОНЕЦ ===

        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("Author with ID " + request.getAuthorId() + " not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + request.getCategoryId() + " not found"));

        News news = newsMapper.toNews(request);
        news.setAuthor(author);
        news.setCategory(category);

        News savedNews = newsRepository.save(news);
        return newsMapper.toNewsResponseWithComments(savedNews);
    }

    @Override
    @Transactional
    public NewsResponse update(Long id, NewsUpdateRequest request) {
        log.info("Updating news with id: {}", id);
        News newsToUpdate = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));

        newsMapper.updateNewsFromRequest(request, newsToUpdate);

        News updatedNews = newsRepository.save(newsToUpdate);
        return newsMapper.toNewsResponseWithComments(updatedNews);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting news with id: {}", id);
        if (!newsRepository.existsById(id)) {
            throw new ResourceNotFoundException("News not found with id: " + id);
        }
        newsRepository.deleteById(id);
    }
}
