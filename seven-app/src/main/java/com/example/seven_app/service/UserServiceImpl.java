package com.example.seven_app.service;

import com.example.seven_app.dto.UserDto;
import com.example.seven_app.mapper.UserMapper;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Flux<UserDto> findAll() {
        return userRepository.findAll()
                .map(userMapper::toDto);
    }

    @Override
    public Mono<UserDto> findById(String id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    @Override
    public Mono<UserDto> save(UserDto userDto) {
        User user = userMapper.toUser(userDto);
        return userRepository.save(user)
                .map(userMapper::toDto);
    }

    @Override
    public Mono<UserDto> update(String id, UserDto userDto) {
        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    existingUser.setUsername(userDto.getUsername());
                    existingUser.setEmail(userDto.getEmail());
                    return userRepository.save(existingUser);
                })
                .map(userMapper::toDto);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return userRepository.deleteById(id);
    }
}
