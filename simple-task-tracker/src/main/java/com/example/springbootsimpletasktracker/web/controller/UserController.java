package com.example.springbootsimpletasktracker.web.controller;

import com.example.springbootsimpletasktracker.entity.RoleType;
import com.example.springbootsimpletasktracker.entity.User;
import com.example.springbootsimpletasktracker.mapper.UserMapper;
import com.example.springbootsimpletasktracker.service.UserService;
import com.example.springbootsimpletasktracker.web.model.request.UpsertUserRequest;
import com.example.springbootsimpletasktracker.web.model.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER')")
    public Flux<UserResponse> findAll() {
        return userService.findAll()
                .map(userMapper::userToResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER')")
    public Mono<ResponseEntity<UserResponse>> findById(@PathVariable String id) {
        return userService.findById(id)
                .map(it -> ResponseEntity.ok(userMapper.userToResponse(it)));
    }

    @PostMapping
    public Mono<ResponseEntity<UserResponse>> createUser(@RequestBody UpsertUserRequest request,
                                                         @RequestParam RoleType role) {
        User user = userMapper.requestToUser(request);
        user.addRole(role);
        Mono<User> newUser = userService.save(user);

        return newUser.map(it -> ResponseEntity.status(HttpStatus.CREATED).body(userMapper.userToResponse(it)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER')")
    public Mono<ResponseEntity<UserResponse>> updateUser(@RequestBody UpsertUserRequest request,
                                                         @PathVariable String id) {
        Mono<User> updatedUser = userService.update(userMapper.requestToUser(request), id);

        return updatedUser.map(it -> ResponseEntity.ok(userMapper.userToResponse(it)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable String id) {
        return userService.deleteById(id);
    }

}
