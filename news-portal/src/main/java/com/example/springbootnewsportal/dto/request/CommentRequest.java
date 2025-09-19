package com.example.springbootnewsportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Comment text cannot be empty")
    private String text;

    @NotNull(message = "Author ID cannot be null")
    private Long authorId;

    @NotNull(message = "News ID cannot be null")
    private Long newsId;

}
