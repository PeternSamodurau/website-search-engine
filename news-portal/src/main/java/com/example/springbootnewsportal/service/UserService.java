package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.UserRequest;
import com.example.springbootnewsportal.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> findAll(Pageable pageable);

    UserResponse findById(Long id);

    UserResponse create(UserRequest request);

    UserResponse update(Long id, UserRequest request);

    void deleteById(Long id);
}
