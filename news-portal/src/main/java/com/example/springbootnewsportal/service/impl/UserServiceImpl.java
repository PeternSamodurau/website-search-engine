package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.UserRepository;
import com.example.springbootnewsportal.service.UserService;
import com.example.springbootnewsportal.dto.request.UserRequest;
import com.example.springbootnewsportal.dto.response.UserResponse;
import com.example.springbootnewsportal.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        log.info("Executing findAll users request");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        log.info("Executing findById request for user with ID: {}", id);
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with ID: " + id);
                });
    }

    @Override
    public UserResponse create(UserRequest request) {
        log.info("Executing create request for new user with username: '{}'", request.getUsername());
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        log.info("Successfully created user with ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {
        log.info("Executing update request for user with ID: {}", id);
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot update. User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with ID: " + id);
                });

        userMapper.updateUserFromRequest(request, existingUser);

        if (StringUtils.hasText(request.getPassword())) {
            log.info("Updating password for user with ID: {}", id);
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        log.info("Successfully updated user with ID: {}", updatedUser.getId());
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteById(Long id) {
        log.info("Executing deleteById request for user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            log.error("Cannot delete. User not found with ID: {}", id);
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
        log.info("Successfully deleted user with ID: {}", id);
    }
}
