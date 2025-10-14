package com.example.seven_app.controller;

import com.example.seven_app.dto.UserDto;
import com.example.seven_app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // <-- Добавлен импорт
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j // <-- Добавлена аннотация
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Flux<UserDto> findAll() {
        log.info("Request to find all users");
        return userService.findAll()
                .doOnError(error -> log.error("Error finding all users", error));
    }

    @GetMapping("/{id}")
    public Mono<UserDto> findById(@PathVariable String id) {
        log.info("Request to find user by id: {}", id);
        return userService.findById(id)
                .doOnError(error -> log.error("Error finding user by id: {}", id, error));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDto> save(@RequestBody UserDto userDto) {
        log.info("Request to save user: {}", userDto);
        return userService.save(userDto)
                .doOnError(error -> log.error("Error saving user: {}", userDto, error));
    }

    @PutMapping("/{id}")
    public Mono<UserDto> update(@PathVariable String id, @RequestBody UserDto userDto) {
        log.info("Request to update user by id: {} with data: {}", id, userDto);
        return userService.update(id, userDto)
                .doOnError(error -> log.error("Error updating user by id: {}", id, error));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(@PathVariable String id) {
        log.info("Request to delete user by id: {}", id); // <-- Положительный лог
        return userService.deleteById(id)
                .doOnError(error -> log.error("Error deleting user by id: {}", id, error)); // <-- Отрицательный лог
    }
}
