package com.example.booksManagement.client.openlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibrarySearchResponse {
    private List<OpenLibraryBookDoc> docs;
}
