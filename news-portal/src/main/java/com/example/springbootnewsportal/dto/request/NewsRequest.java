package com.example.springbootnewsportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewsRequest {

    @NotBlank(message = "News title cannot be empty")
    private String title;

    @NotBlank(message = "News content cannot be empty")
    private String text; // <--- ПОЛЕ ПЕРЕИМЕНОВАНО

    @NotNull(message = "Category ID cannot be null")
    private Long categoryId;

    @NotNull(message = "Author ID cannot be null")
    private Long authorId;
}
