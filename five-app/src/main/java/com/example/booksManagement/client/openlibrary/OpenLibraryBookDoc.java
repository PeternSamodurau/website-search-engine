package com.example.booksManagement.client.openlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryBookDoc {
    private String title;

    @JsonProperty("author_name")
    private List<String> authorName;

    @JsonProperty("subject")
    private List<String> subject;
}