package com.example.seven_app.service;

import com.example.seven_app.dto.response.UserResponseDto;
import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.mapper.UserMapper;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Flux<UserResponseDto> findAll() {
        return userRepository.findAll()
                .map(userMapper::toDto);
    }

    @Override
    public Mono<UserResponseDto> findById(String id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    @Override
    public Mono<UserResponseDto> save(UserRequestDto userRequestDto) {
        return userRepository.existsByUsername(userRequestDto.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists"));
                    }
                    return userRepository.existsByEmail(userRequestDto.getEmail());
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists"));
                    }
                    User user = userMapper.toUser(userRequestDto);
                    return userRepository.save(user).map(userMapper::toDto);
                });
    }

    @Override
    public Mono<UserResponseDto> update(String id, UserRequestDto userRequestDto) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(existingUser -> {
                    Mono<Void> checkUsername = Mono.empty();
                    if (!existingUser.getUsername().equals(userRequestDto.getUsername())) {
                        checkUsername = userRepository.existsByUsername(userRequestDto.getUsername())
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken"));
                                    }
                                    return Mono.empty();
                                });
                    }

                    Mono<Void> checkEmail = Mono.empty();
                    if (!existingUser.getEmail().equals(userRequestDto.getEmail())) {
                        checkEmail = userRepository.existsByEmail(userRequestDto.getEmail())
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already taken"));
                                    }
                                    return Mono.empty();
                                });
                    }

                    return checkUsername.then(checkEmail)
                            .then(Mono.fromRunnable(() -> {
                                existingUser.setUsername(userRequestDto.getUsername());
                                existingUser.setEmail(userRequestDto.getEmail());
                            }))
                            .then(userRepository.save(existingUser));
                })
                .map(userMapper::toDto);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return userRepository.deleteById(id);
    }
}
