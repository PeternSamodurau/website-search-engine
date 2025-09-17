package com.example.springbootnewsportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {

    @NotBlank(message = "Comment text cannot be empty")
    private String text;

    @NotNull(message = "News ID cannot be null")
    private Long newsId;

    @NotNull(message = "Author ID cannot be null")
    private Long authorId;
}
