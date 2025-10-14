package com.example.seven_app.service;

import com.example.seven_app.dto.UserDto;
import com.example.seven_app.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<UserDto> findAll();

    Mono<UserDto> findById(String id);

    Mono<UserDto> save(UserDto userDto);

    Mono<UserDto> update(String id, UserDto userDto);

    Mono<Void> deleteById(String id);
}
