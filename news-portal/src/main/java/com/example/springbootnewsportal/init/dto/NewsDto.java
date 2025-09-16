package com.example.springbootnewsportal.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsDto {

    private String sourceName;

    private String author;
    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private Instant publishedAt;
    private String content;

    @JsonProperty("source")
    private void unpackSource(Map<String, Object> source) {
        if (source != null) {
            this.sourceName = (String) source.get("name");
        }
    }
}
