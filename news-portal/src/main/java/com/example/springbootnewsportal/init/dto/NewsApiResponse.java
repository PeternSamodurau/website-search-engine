package com.example.springbootnewsportal.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // Игнорирует ненужные поля (status, totalResults)
public class NewsApiResponse {
    private String status;
    private Integer totalResults;
    private List<NewsDto> articles;
}
