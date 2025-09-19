package com.example.springbootnewsportal.dto.response;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String categoryName; // <--- ПОЛЕ ПЕРЕИМЕНОВАНО
    private Long newsCount;
}
