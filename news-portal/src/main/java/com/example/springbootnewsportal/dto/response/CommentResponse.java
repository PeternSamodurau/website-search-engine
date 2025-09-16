package com.example.springbootnewsportal.dto.response;

import lombok.Data;

@Data
public class CommentResponse {
    private Long id;
    private String commentText;
    private String authorUsername; // <-- ДОБАВЛЕНО
}
