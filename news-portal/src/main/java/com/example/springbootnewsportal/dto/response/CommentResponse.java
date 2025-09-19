package com.example.springbootnewsportal.dto.response;

import lombok.Data;

@Data
public class CommentResponse {
    private Long id;
    private String text; // <--- ПОЛЕ ПЕРЕИМЕНОВАНО
    private String authorUsername;
}
