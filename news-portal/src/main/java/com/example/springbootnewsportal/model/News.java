package com.example.springbootnewsportal.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class News {
    private Long id;
    private String title;
    private String content;
    private String category;
}
