package com.example.seven_app.controller;

import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.dto.response.UserResponseDto;
import com.example.seven_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "UserController", description = "Контроллер для работы с пользователями")
@Slf4j
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получение списка всех пользователей (доступно для ROLE_USER и ROLE_MANAGER)")
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Flux<UserResponseDto> getAllUsers() {
        log.info("Request to get all users");
        return userService.findAll();
    }

    @Operation(summary = "Получение пользователя по ID (доступно для ROLE_USER и ROLE_MANAGER)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<UserResponseDto> getUserById(@PathVariable String id) {
        log.info("Request to get user by id: {}", id);
        return userService.findById(id)
                .doOnSuccess(user -> log.info("Successfully found user: {}", user));
    }

    @Operation(summary = "Создание нового пользователя (доступно только для ROLE_MANAGER)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        log.info("Request to save user: {}", request);
        return userService.save(request);
    }

    @Operation(summary = "Обновление пользователя по ID (доступно для ROLE_USER и ROLE_MANAGER. Смена имени и пароля, авторизуйтесь заново!)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<UserResponseDto> updateUser(@PathVariable String id, @Valid @RequestBody UserRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to update user by id: {}", id);
        return userService.update(id, request, userDetails);
    }

    @Operation(summary = "Удаление пользователя по ID (доступно для ROLE_USER и ROLE_MANAGER. Не удалите самого себя когда в системе!")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<Map<String, String>> deleteUser(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to delete user by id: {}", id);
        return userService.deleteById(id, userDetails)
                .then(Mono.just(Map.of("message", "Пользователь с ID " + id + " успешно удален")));
    }
}