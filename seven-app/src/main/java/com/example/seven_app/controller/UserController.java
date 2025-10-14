package com.example.seven_app.controller;

import com.example.seven_app.dto.UserDto;
import com.example.seven_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "User Controller", description = "API для работы с пользователями")
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает полный список всех пользователей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователи успешно получены",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserDto.class))))
    })
    @GetMapping
    public Flux<UserDto> findAll() {
        log.info("Request to find all users");
        return userService.findAll()
                .doOnError(error -> log.error("Error finding all users", error));
    }

    @Operation(summary = "Получить пользователя по ID", description = "Возвращает пользователя по его уникальному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content)
    })
    @GetMapping("/{id}")
    public Mono<UserDto> findById(@PathVariable String id) {
        log.info("Request to find user by id: {}", id);
        return userService.findById(id)
                .doOnError(error -> log.error("Error finding user by id: {}", id, error));
    }

    @Operation(summary = "Создать нового пользователя", description = "Создает нового пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDto> save(@RequestBody UserDto userDto) {
        log.info("Request to save user: {}", userDto);
        return userService.save(userDto)
                .doOnError(error -> log.error("Error saving user: {}", userDto, error));
    }

    @Operation(summary = "Обновить существующего пользователя", description = "Обновляет данные существующего пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content)
    })
    @PutMapping("/{id}")
    public Mono<UserDto> update(@PathVariable String id, @RequestBody UserDto userDto) {
        log.info("Request to update user by id: {} with data: {}", id, userDto);
        return userService.update(id, userDto)
                .doOnError(error -> log.error("Error updating user by id: {}", id, error));
    }

    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(@PathVariable String id) {
        log.info("Request to delete user by id: {}", id);
        return userService.deleteById(id)
                .doOnError(error -> log.error("Error deleting user by id: {}", id, error));
    }
}
