package com.example.springbootnewsportal.service.impl;

import com.example.springbootnewsportal.exception.ResourceNotFoundException;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.UserRepository;
import com.example.springbootnewsportal.service.UserService;
import com.example.springbootnewsportal.dto.request.UserRequest;
import com.example.springbootnewsportal.dto.response.UserResponse;
import com.example.springbootnewsportal.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Override
    public UserResponse create(UserRequest request) {
        User user = userMapper.toUser(request);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        userMapper.updateUserFromRequest(request, existingUser);

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }
}