package com.example.seven_app.service;

import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.dto.response.UserResponseDto;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<UserResponseDto> findAll();

    Mono<UserResponseDto> findById(String id);

    Mono<UserResponseDto> save(UserRequestDto userRequestDto);

    Mono<UserResponseDto> update(String id, UserRequestDto userRequestDto, UserDetails userDetails);

    Mono<Void> deleteById(String id, UserDetails userDetails);
}
