package com.example.seven_app.service;

import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.dto.response.UserResponseDto;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
        return userRepository.findById(id)
                .flatMap(user -> {
                    boolean isManager = userDetails.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
                    boolean isOwner = user.getUsername().equals(userDetails.getUsername());

                    if (!isManager && !isOwner) {
                        return Mono.error(new AccessDeniedException("User is not authorized to update this profile"));
                    }

                    user.setUsername(userRequestDto.getUsername());
                    user.setEmail(userRequestDto.getEmail());
                    if (userRequestDto.getPassword() != null && !userRequestDto.getPassword().isBlank()) {
                        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
                    }
                    return userRepository.save(user);
                })
                .map(this::toDto);
    }

    @Override
    public Mono<Void> deleteById(String id, UserDetails userDetails) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    boolean isManager = userDetails.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
                    boolean isOwner = user.getUsername().equals(userDetails.getUsername());

                    if (!isManager && !isOwner) {
                        return Mono.error(new AccessDeniedException("User is not authorized to delete this profile"));
                    }
                    return userRepository.deleteById(user.getId());
                });
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsernameOrEmail(username, username)
                .cast(UserDetails.class);
    }
}
