package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.request.CommentUpdateRequest; // <--- ИЗМЕНЕНИЕ
import com.example.springbootnewsportal.dto.response.CommentResponse;
import com.example.springbootnewsportal.service.CommentService;
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

@Tag(name = "Комментарии", description = "Операции для управления комментариями")
@RestController
@RequestMapping("/api/v1/news/{newsId}/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    // ... (методы createComment и getCommentsByNewsId без изменений) ...
    @Operation(
            summary = "Создать новый комментарий",
            description = "Добавляет новый комментарий к новости, принимая текст и ID автора как отдельные поля."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Комментарий успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость или автор с таким ID не найдены", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "ID новости, к которой добавляется комментарий", required = true) @PathVariable Long newsId,
            @Parameter(description = "Текст комментария", required = true) @RequestParam String text,
            @Parameter(description = "ID автора комментария", required = true) @RequestParam Long authorId
    ) {
        log.info("Request to create a new comment for news with id: {} by author: {}", newsId, authorId);
        // Создаем DTO вручную из параметров запроса
        CommentRequest request = new CommentRequest(text, authorId, newsId);
        CommentResponse createdComment = commentService.create(request);
        log.info("Successfully created a new comment with id: {} for news with id: {}. Response code: 201", createdComment.getId(), newsId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

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
        log.info("Request to get all comments for news with id: {}", newsId);
        List<CommentResponse> comments = commentService.findAllByNewsId(newsId);
        log.info("Successfully retrieved {} comments for news with id: {}. Response code: 200", comments.size(), newsId);
        return ResponseEntity.ok(comments);
    }

    // === БЛОК ИЗМЕНЕНИЙ НАЧАЛО ===
    @Operation(
            summary = "Обновить комментарий",
            description = "Обновляет существующий комментарий."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Комментарий успешно обновлен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден", content = @Content)
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID новости") @PathVariable Long newsId,
            @Parameter(description = "ID комментария") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request // <--- ИЗМЕНЕНИЕ
    ) {
        log.info("Request to update comment with id: {}", commentId);
        CommentResponse updatedComment = commentService.update(commentId, request); // <--- ИЗМЕНЕНИЕ
        log.info("Successfully updated comment with id: {}. Response code: 200", commentId);
        return ResponseEntity.ok(updatedComment);
    }
    // === БЛОК ИЗМЕНЕНИЙ КОНЕЦ ===

    @Operation(
            summary = "Удалить комментарий",
            description = "Удаляет комментарий по его ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Комментарий успешно удален", content = @Content),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден", content = @Content)
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "ID новости") @PathVariable Long newsId,
            @Parameter(description = "ID комментария") @PathVariable Long commentId
    ) {
        log.info("Request to delete comment with id: {}", commentId);
        commentService.deleteById(commentId);
        log.info("Successfully deleted comment with id: {}. Response code: 204", commentId);
        return ResponseEntity.noContent().build();
    }
}
