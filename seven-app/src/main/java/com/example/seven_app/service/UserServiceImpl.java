package com.example.seven_app.service;

import com.example.seven_app.dto.response.UserResponseDto;
import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.mapper.UserMapper;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Flux<UserResponseDto> findAll() {
        log.info("Request to find all users");
        return userRepository.findAll()
                .map(userMapper::toDto)
                .doOnComplete(() -> log.info("Successfully found all users"))
                .doOnError(error -> log.error("Error while finding all users: {}", error.getMessage()));
    }

    @Override
    public Mono<UserResponseDto> findById(String id) {
        log.info("Request to find user by id: {}", id);
        return userRepository.findById(id)
                .filter(Objects::nonNull)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + id + " not found")))
                .map(userMapper::toDto)
                .doOnSuccess(user -> log.info("Successfully found user: {}", user))
                .doOnError(error -> log.error("Error while finding user by id {}: {}", id, error.getMessage()));
    }

    @Override
    public Mono<UserResponseDto> save(UserRequestDto userRequestDto) {
        log.info("Request to save user: {}", userRequestDto);
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
                })
                .doOnSuccess(user -> log.info("Successfully saved user: {}", user))
                .doOnError(error -> log.error("Error while saving user {}: {}", userRequestDto, error.getMessage()));
    }

    @Override
    public Mono<UserResponseDto> update(String id, UserRequestDto userRequestDto) {
        log.info("Request to update user with id {}: {}", id, userRequestDto);

        // ШАГ 1: Найти пользователя по ID. Это самое первое действие.
        return userRepository.findById(id)
                // ШАГ 2: Если пользователь на ШАГЕ 1 не найден, немедленно вернуть ошибку 404.
                // Никакие проверки email или username НЕ выполняются.
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + id + " not found")))

                // ШАГ 3: Этот блок выполняется ТОЛЬКО ЕСЛИ пользователь на ШАГЕ 1 был найден.
                .flatMap(existingUser -> {

                    // ШАГ 3.1: Теперь, когда мы знаем, что пользователь существует, проверяем,
                    // не заняты ли новое имя или почта ДРУГИМ пользователем.
                    return userRepository.findByUsernameOrEmail(userRequestDto.getUsername(), userRequestDto.getEmail())
                            // Исключаем из проверки самого себя (пользователя, которого мы обновляем).
                            .filter(foundUser -> !foundUser.getId().equals(existingUser.getId()))

                            // ШАГ 3.2: Этот блок выполняется, ТОЛЬКО ЕСЛИ нашелся ДРУГОЙ пользователь с таким же именем или почтой.
                            .flatMap(conflictingUser -> {
                                // Определяем, что именно занято, и возвращаем ошибку 409.
                                if (conflictingUser.getUsername().equals(userRequestDto.getUsername())) {
                                    return Mono.<User>error(new ResponseStatusException(HttpStatus.CONFLICT, "Username " + userRequestDto.getUsername() + " is already taken"));
                                } else {
                                    return Mono.<User>error(new ResponseStatusException(HttpStatus.CONFLICT, "Email " + userRequestDto.getEmail() + " is already taken"));
                                }
                            })

                            // ШАГ 3.3: Этот блок выполняется, ТОЛЬКО ЕСЛИ на ШАГЕ 3.1 не нашлось конфликтов.
                            // Это означает, что имя и почта свободны.
                            .switchIfEmpty(Mono.defer(() -> {
                                // Обновляем данные и сохраняем пользователя.
                                existingUser.setUsername(userRequestDto.getUsername());
                                existingUser.setEmail(userRequestDto.getEmail());
                                return userRepository.save(existingUser);
                            }));
                })
                .map(userMapper::toDto)
                .doOnSuccess(updatedUser -> log.info("Successfully updated user: {}", updatedUser))
                .doOnError(error -> log.error("Error while updating user with id {}: {}", id, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        log.info("Request to delete user by id: {}", id);
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + id + " not found")))
                .flatMap(userRepository::delete)
                .doOnSuccess(v -> log.info("Successfully deleted user with id: {}", id))
                .doOnError(error -> log.error("Error while deleting user with id {}: {}", id, error.getMessage()));
    }
}
