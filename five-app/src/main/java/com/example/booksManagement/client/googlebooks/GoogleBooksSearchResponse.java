package com.example.booksManagement.client.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksSearchResponse {
    private List<VolumeItem> items;
}
