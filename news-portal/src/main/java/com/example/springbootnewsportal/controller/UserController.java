package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.UserRequest;
import com.example.springbootnewsportal.dto.response.UserResponse;
import com.example.springbootnewsportal.service.UserService;
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

@Tag(name = "Пользователи", description = "Операции для управления пользователями и их данными.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает постраничный список всех пользователей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен")
    })
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @Operation(summary = "Получить пользователя по ID", description = "Возвращает одного пользователя по его уникальному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "Уникальный идентификатор пользователя") @PathVariable Long id
    ) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Создать нового пользователя", description = "Создает нового пользователя. Обычно это эндпоинт для регистрации.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody(description = "Данные для создания нового пользователя") @Valid @org.springframework.web.bind.annotation.RequestBody UserRequest request
    ) {
        UserResponse createdUser = userService.create(request);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить пользователя", description = "Обновляет данные существующего пользователя по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID пользователя, которого нужно обновить") @PathVariable Long id,
            @RequestBody(description = "Новые данные для пользователя") @Valid @org.springframework.web.bind.annotation.RequestBody UserRequest request
    ) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя, которого нужно удалить") @PathVariable Long id
    ) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
