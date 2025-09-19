package com.example.springbootnewsportal.service;

import com.example.springbootnewsportal.dto.request.UserRequest;
import com.example.springbootnewsportal.dto.response.UserResponse;

import java.util.List; // <-- ИЗМЕНЕНИЕ

public interface UserService {


    List<UserResponse> findAll();

    UserResponse findById(Long id);

    UserResponse create(UserRequest request);

    UserResponse update(Long id, UserRequest request);

    void deleteById(Long id);
}
