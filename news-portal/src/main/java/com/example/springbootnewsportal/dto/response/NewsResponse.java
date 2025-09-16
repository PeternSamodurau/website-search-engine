package com.example.springbootnewsportal.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class NewsResponse {
    private Long id;
    private String title;
    private String content;
    private String authorUsername; // <-- ДОБАВЛЕНО
    private String categoryName;   // <-- ДОБАВЛЕНО
    private Long commentsCount;    // <-- ДОБАВЛЕНО
    private List<CommentResponse> comments;
}
