package com.example.springbootnewsportal.dto.response;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private Long newsCount;     // <-- ДОБАВЛЕНО
    private Long commentsCount; // <-- ДОБАВЛЕНО
}
