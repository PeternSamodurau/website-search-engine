package com.example.seven_app.controller;

import com.example.seven_app.dto.response.UserResponseDto;
import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Controller", description = "Операции с пользователями")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователи успешно получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDto.class)))
    })
    @GetMapping
    public Flux<UserResponseDto> getAllUsers() {
        log.info("Request to get all users");
        return userService.findAll();
    }

    @Operation(summary = "Получить пользователя по ID", description = "Возвращает одного пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content)
    })
    @GetMapping("/{id}")
    public Mono<UserResponseDto> getUserById(@PathVariable String id) {
        log.info("Request to get user by id: {}", id);
        return userService.findById(id);
    }

    @Operation(summary = "Создать нового пользователя", description = "Создает нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректное тело запроса", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        log.info("Request to save user: {}", request);
        return userService.save(request);
    }

    @Operation(summary = "Обновить существующего пользователя", description = "Обновляет существующего пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "400", description = "Некорректное тело запроса", content = @Content)
    })
    @PutMapping("/{id}")
    public Mono<UserResponseDto> updateUser(@PathVariable String id, @Valid @RequestBody UserRequestDto request) {
        log.info("Request to update user with id: {}", id);
        return userService.update(id, request);
    }

    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable String id) {
        log.info("Request to delete user with id: {}", id);
        return userService.deleteById(id);
    }
}
