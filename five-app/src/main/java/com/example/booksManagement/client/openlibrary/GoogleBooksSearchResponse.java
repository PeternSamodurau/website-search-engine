package com.example.booksManagement.client.openlibrary;


import com.example.booksManagement.client.googlebooks.VolumeItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksSearchResponse {
    private List<VolumeItem> items;
}