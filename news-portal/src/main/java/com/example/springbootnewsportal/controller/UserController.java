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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Пользователи", description = "Операции для управления пользователями и их данными.")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен")
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Request to get all users");

        List<UserResponse> users = userService.findAll();

        log.info("Successfully retrieved {} users. Response code: 200", users.size());
        return ResponseEntity.ok(users);
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
        log.info("Request to get user with id: {}", id);

        UserResponse user = userService.findById(id);

        log.info("Successfully retrieved user with id: {}. Response code: 200", id);
        return ResponseEntity.ok(user);
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
        log.info("Request to create a new user");

        UserResponse createdUser = userService.create(request);

        log.info("Successfully created a new user with id: {}. Response code: 201", createdUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @Operation(summary = "Обновить пользователя", description = "Обновляет данные существующего пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID пользователя, которого нужно обновить") @PathVariable Long id,
            @RequestBody(description = "Новые данные для пользователя") @Valid @org.springframework.web.bind.annotation.RequestBody UserRequest request
    ) {
        log.info("Request to update user with id: {}", id);

        UserResponse updatedUser = userService.update(id, request);

        log.info("Successfully updated user with id: {}. Response code: 200", id);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя, которого нужно удалить") @PathVariable Long id
    ) {
        log.info("Request to delete user with id: {}", id);

        userService.deleteById(id);

        log.info("Successfully deleted user with id: {}. Response code: 204", id);
        return ResponseEntity.noContent().build();
    }
}
