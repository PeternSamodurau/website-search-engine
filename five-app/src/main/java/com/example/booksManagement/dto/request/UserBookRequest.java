package com.example.booksManagement.dto.request;

import lombok.Data;

@Data
public class UserBookRequest {
    private String title;
    private String author;
    private String categoryName;
}
