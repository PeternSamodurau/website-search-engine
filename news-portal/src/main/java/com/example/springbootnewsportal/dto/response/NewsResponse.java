package com.example.springbootnewsportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {

    private Long id;
    private String title;
    private String text;
    private Instant createAt;
    private Instant updateAt;
    private String authorUsername;
    private String categoryName; // <--- ПОЛЕ ПЕРЕИМЕНОВАНО
    private Long commentsCount;
    private List<CommentResponse> comments = new ArrayList<>();
}
