package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Новости", description = "Операции с новостями")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "Получить список новостей", description = "Возвращает список новостей с возможностью фильтрации по автору и категории.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новости успешно получены",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NewsResponse.class))))
    })
    @GetMapping
    public ResponseEntity<List<NewsResponse>> getAllNews(
            @Parameter(description = "ID автора для фильтрации") @RequestParam(required = false) Long authorId,
            @Parameter(description = "ID категории для фильтрации") @RequestParam(required = false) Long categoryId) {
        log.info("Request to get all news with authorId: {} and categoryId: {}", authorId, categoryId);
        List<NewsResponse> news = newsService.findAll(authorId, categoryId);
        log.info("Successfully retrieved {} news items", news.size());
        return ResponseEntity.ok(news);
    }

    @Operation(summary = "Получить новость по ID", description = "Возвращает новость и все ее комментарии по ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость успешно найдена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        log.info("Request to get news with id: {}", id);
        NewsResponse news = newsService.findById(id);
        log.info("Successfully retrieved news with id: {}", id);
        return ResponseEntity.ok(news);
    }

    @Operation(summary = "Создать новую новость", description = "Создает новую новость.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Новость успешно создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content)
    })
    @PostMapping
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request) {
        log.info("Request to create a new news with title: '{}'", request.getTitle());
        NewsResponse createdNews = newsService.create(request);
        log.info("Successfully created a new news with id: {}", createdNews.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNews);
    }

    @Operation(summary = "Обновить новость", description = "Обновляет существующую новость по ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content),
            @ApiResponse(responseCode = "403", description = "Нет прав для редактирования этой новости", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<NewsResponse> updateNews(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        log.info("Request to update news with id: {}", id);
        NewsResponse updatedNews = newsService.update(id, request);
        log.info("Successfully updated news with id: {}", id);
        return ResponseEntity.ok(updatedNews);
    }

    @Operation(summary = "Удалить новость", description = "Удаляет новость по ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Новость успешно удалена", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content),
            @ApiResponse(responseCode = "403", description = "Нет прав для удаления этой новости", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        log.info("Request to delete news with id: {}", id);
        newsService.deleteById(id);
        log.info("Successfully deleted news with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}