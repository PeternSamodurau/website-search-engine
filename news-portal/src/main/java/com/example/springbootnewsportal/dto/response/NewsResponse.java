package com.example.springbootnewsportal.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class NewsResponse {
    private Long id;
    private String title;
    private String content;
    private String authorUsername;
    private String categoryName;
    private Long commentsCount;
    private List<CommentResponse> comments;
}
