package com.example.seven_app.service;

import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.dto.response.UserResponseDto;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private UserResponseDto toDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }

    @Override
    public Flux<UserResponseDto> findAll() {
        return userRepository.findAll().map(this::toDto);
    }

    @Override
    public Mono<UserResponseDto> findById(String id) {
        return userRepository.findById(id).map(this::toDto);
    }


    @Override
    public Mono<UserResponseDto> save(UserRequestDto userRequestDto) {
        User user = new User();
        user.setUsername(userRequestDto.getUsername());
        user.setEmail(userRequestDto.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        return userRepository.save(user).map(this::toDto);
    }

    @Override
    public Mono<UserResponseDto> update(String id, UserRequestDto userRequestDto, UserDetails userDetails) {
        User requester = (User) userDetails;
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " not found")))
                .flatMap(userToUpdate -> {
                    boolean isManager = requester.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_MANAGER"));
                    boolean isOwner = requester.getId().equals(userToUpdate.getId());

                    if (isManager || isOwner) {
                        userToUpdate.setUsername(userRequestDto.getUsername());
                        userToUpdate.setEmail(userRequestDto.getEmail());
                        if (userRequestDto.getPassword() != null && !userRequestDto.getPassword().isBlank()) {
                            userToUpdate.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
                        }
                        return userRepository.save(userToUpdate);
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied"));
                    }
                })
                .map(this::toDto);
    }

    @Override
    public Mono<Void> deleteById(String id, UserDetails userDetails) {
        User requester = (User) userDetails;
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " not found")))
                .flatMap(userToDelete -> {
                    boolean isManager = requester.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_MANAGER"));
                    boolean isOwner = requester.getId().equals(userToDelete.getId());

                    if (isManager || isOwner) {
                        return userRepository.delete(userToDelete);
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied"));
                    }
                });
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsernameOrEmail(username, username)
                .cast(UserDetails.class);
    }
}
