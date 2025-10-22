package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.request.CommentUpdateRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "Комментарии", description = "Операции для управления комментариями")
@RestController
@RequestMapping("/api/v1/news/{newsId}/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "Создать новый комментарий",
            description = "Добавляет новый комментарий к новости. ID автора определяется по токену аутентификации."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Комментарий успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "ID новости, к которой добавляется комментарий", required = true) @PathVariable Long newsId,
            @Valid @RequestBody CommentRequest request,
            Principal principal
    ) {
        log.info("Request to create a new comment for news with id: {} by user: {}", newsId, principal.getName());

        CommentResponse createdComment = commentService.create(newsId, request, principal);

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
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Новость с таким ID не найдена", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<List<CommentResponse>> getCommentsByNewsId(
            @Parameter(description = "ID новости, для которой нужно получить комментарии") @PathVariable Long newsId
    ) {
        log.info("Request to get all comments for news with id: {}", newsId);

        List<CommentResponse> comments = commentService.findAllByNewsId(newsId);

        log.info("Successfully retrieved {} comments for news with id: {}. Response code: 200", comments.size(), newsId);
        return ResponseEntity.ok(comments);
    }

    @Operation(
            summary = "Обновить комментарий",
            description = "Обновляет существующий комментарий. Доступно только автору комментария."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Комментарий успешно обновлен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не автор)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден", content = @Content)
    })
    @PutMapping("/{commentId}")
    @PreAuthorize("@commentServiceImpl.isCommentAuthor(#commentId, principal.username)")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID новости") @PathVariable Long newsId,
            @Parameter(description = "ID комментария") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        log.info("Request to update comment with id: {}", commentId);

        CommentResponse updatedComment = commentService.update(commentId, request);

        log.info("Successfully updated comment with id: {}. Response code: 200", commentId);
        return ResponseEntity.ok(updatedComment);
    }

    @Operation(
            summary = "Удалить комментарий",
            description = "Удаляет комментарий по его ID. Доступно автору, администратору или модератору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Комментарий успешно удален", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден", content = @Content)
    })
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MODERATOR') or @commentServiceImpl.isCommentAuthor(#commentId, principal.username)")
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