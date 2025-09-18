package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Новости", description = "Операции для управления новостями. Позволяет создавать, искать, обновлять и удалять новости.")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(
            summary = "Найти все новости",
            description = "Возвращает постраничный список всех новостей. Позволяет фильтровать по автору и/или категории."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список новостей успешно получен",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)) }) // <-- ИСПРАВЛЕНО
    })
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getAllNews(
            @Parameter(description = "ID автора для фильтрации новостей") @RequestParam(required = false) Long authorId,
            @Parameter(description = "ID категории для фильтрации новостей") @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        return ResponseEntity.ok(newsService.findAll(authorId, categoryId, pageable));
    }

    @Operation(
            summary = "Получить новость по ID",
            description = "Возвращает полную информацию об одной новости по ее уникальному идентификатору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость найдена",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class)) }), // <-- ИСПРАВЛЕНО
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(
            @Parameter(description = "Уникальный идентификатор новости") @PathVariable Long id
    ) {
        return ResponseEntity.ok(newsService.findById(id));
    }

    @Operation(
            summary = "Создать новую новость",
            description = "Создает новую новость. Требует аутентификации. Автор новости назначается автоматически на основе текущего пользователя."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Новость успешно создана",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (пользователь не аутентифицирован)", content = @Content)
    })
    @PostMapping
    public ResponseEntity<NewsResponse> createNews(
            @RequestBody(description = "Объект с данными для создания новости") @Valid @org.springframework.web.bind.annotation.RequestBody NewsRequest request
    ) {
        NewsResponse createdNews = newsService.create(request);
        return new ResponseEntity<>(createdNews, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Обновить новость",
            description = "Обновляет существующую новость по ее ID. Требуется аутентификация и право собственности на новость."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость успешно обновлена",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = NewsResponse.class)) }), // <-- ИСПРАВЛЕНО
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (нет прав на редактирование)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<NewsResponse> updateNews(
            @Parameter(description = "ID новости, которую нужно обновить") @PathVariable Long id,
            @RequestBody(description = "Новые данные для новости") @Valid @org.springframework.web.bind.annotation.RequestBody NewsRequest request
    ) {
        return ResponseEntity.ok(newsService.update(id, request));
    }

    @Operation(
            summary = "Удалить новость",
            description = "Удаляет новость по ее ID. Требуется аутентификация и право собственности на новость."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Новость успешно удалена", content = @Content), // 204 No Content не имеет тела
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (нет прав на удаление)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(
            @Parameter(description = "ID новости, которую нужно удалить") @PathVariable Long id
    ) {
        newsService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
