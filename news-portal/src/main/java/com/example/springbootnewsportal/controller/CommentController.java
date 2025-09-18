package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;
import com.example.springbootnewsportal.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Комментарии", description = "Операции для управления комментариями к новостям.")
@RestController
@RequestMapping("/api/news/{newsId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "Получить все комментарии к новости",
            description = "Возвращает список всех комментариев для указанной новости."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Комментарии успешно получены",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CommentResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getCommentsByNewsId(
            @Parameter(description = "ID новости, для которой нужно получить комментарии") @PathVariable Long newsId
    ) {
        return ResponseEntity.ok(commentService.findAllByNewsId(newsId));
    }

    @Operation(
            summary = "Создать новый комментарий",
            description = "Добавляет новый комментарий к новости. Требует аутентификации."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Комментарий успешно создан",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (пользователь не аутентифицирован)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "ID новости, к которой добавляется комментарий") @PathVariable Long newsId,
            @RequestBody(description = "Текст комментария") @Valid @org.springframework.web.bind.annotation.RequestBody CommentRequest request
    ) {
        CommentResponse createdComment = commentService.create(newsId, request);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Обновить комментарий",
            description = "Обновляет существующий комментарий по его ID. Требуется аутентификация и право собственности на комментарий."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Комментарий успешно обновлен",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (нет прав на редактирование)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Комментарий или новость не найдены", content = @Content)
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID новости (игнорируется, но нужен для пути)") @PathVariable Long newsId,
            @Parameter(description = "ID комментария, который нужно обновить") @PathVariable Long commentId,
            @RequestBody(description = "Новый текст комментария") @Valid @org.springframework.web.bind.annotation.RequestBody CommentRequest request
    ) {
        return ResponseEntity.ok(commentService.update(commentId, request));
    }

    @Operation(
            summary = "Удалить комментарий",
            description = "Удаляет комментарий по его ID. Требуется аутентификация и право собственности на комментарий."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Комментарий успешно удален", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (нет прав на удаление)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Комментарий или новость не найдены", content = @Content)
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "ID новости (игнорируется, но нужен для пути)") @PathVariable Long newsId,
            @Parameter(description = "ID комментария, который нужно удалить") @PathVariable Long commentId
    ) {
        commentService.deleteById(commentId);
        return ResponseEntity.noContent().build();
    }
}
