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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "UserController", description = "Контроллер для работы с пользователями")
@Slf4j
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей")
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Flux<UserResponseDto> getAllUsers() {
        log.info("Request to get all users");
        return userService.findAll();
    }

    @Operation(summary = "Получить пользователя по ID", description = "Возвращает пользователя по его ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<UserResponseDto> getUserById(@PathVariable String id) {
        log.info("Request to get user by id: {}", id);
        return userService.findById(id);
    }

    @Operation(summary = "Создать нового пользователя", description = "Создает нового пользователя (только для ROLE_MANAGER)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        log.info("Request to save user: {}", request);
        return userService.save(request);
    }

    @Operation(summary = "Обновить пользователя", description = "Обновляет данные пользователя по ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<UserResponseDto> updateUser(@PathVariable String id, @Valid @RequestBody UserRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to update user by id: {}", id);
        return userService.update(id, request, userDetails);
    }

    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<Void> deleteUser(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to delete user by id: {}", id);
        return userService.deleteById(id, userDetails);
    }
}
