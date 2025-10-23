package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.request.NewsUpdateRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Новости", description = "Операции с новостями")
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "Получить все новости с пагинацией и фильтрацией",
            description = "Возвращает страницу с новостями. Можно фильтровать по категории.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новости успешно получены")
    })
    @Parameter(name = "page", description = "Номер страницы (начиная с 0)", in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    @Parameter(name = "size", description = "Количество элементов на странице", in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    @Parameter(name = "sort", description = "Популярные сортировки: 'createAt,desc' (новые сверху), 'commentsCount,desc' (самые обсуждаемые), 'title,asc' (по алфавиту). Формат: поле,asc|desc", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<Page<NewsResponse>> getAllNews(
            @RequestParam(required = false) Long categoryId,
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable) {

        log.info("Request to get all news with filters: categoryId={}, pageable={}", categoryId, pageable);

        Page<NewsResponse> newsPage = newsService.findAll(categoryId, pageable);

        log.info("Successfully retrieved news. Total elements: {}. Response code: 200", newsPage.getTotalElements());
        return ResponseEntity.ok(newsPage);
    }

    @Operation(summary = "Получить новость по ID", description = "Возвращает новость по ее уникальному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость успешно найдена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        log.info("Request to get news with id: {}", id);

        NewsResponse news = newsService.findById(id);

        log.info("Successfully retrieved news with id: {}. Response code: 200", id);
        return ResponseEntity.ok(news);
    }

    @Operation(summary = "Создать новую новость", description = "Создает новую новость.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Новость успешно создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request, Principal principal) {
        log.info("Request to create a new news with title: {}", request.getTitle());

        NewsResponse createdNews = newsService.create(request, principal);

        log.info("Successfully created a new news with id: {}. Response code: 201", createdNews.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNews);
    }

    @Operation(summary = "Обновить существующую новость", description = "Обновляет существующую новость.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("@newsServiceImpl.isNewsAuthor(#id, principal.username)")
    public ResponseEntity<NewsResponse> updateNews(@PathVariable Long id, @Valid @RequestBody NewsUpdateRequest request) {
        log.info("Request to update news with id: {}", id);

        NewsResponse updatedNews = newsService.update(id, request);

        log.info("Successfully updated news with id: {}. Response code: 200", id);
        return ResponseEntity.ok(updatedNews);
    }

    @Operation(summary = "Удалить новость", description = "Удаляет новость по ее ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Новость успешно удалена", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MODERATOR') or @newsServiceImpl.isNewsAuthor(#id, principal.username)")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        log.info("Request to delete news with id: {}", id);

        newsService.deleteById(id);

        log.info("Successfully deleted news with id: {}. Response code: 204", id);
        return ResponseEntity.noContent().build();
    }
}
