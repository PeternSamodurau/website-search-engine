package com.example.booksManagement.client;

import com.example.booksManagement.client.openlibrary.OpenLibrarySearchResponse;
import com.example.booksManagement.configuration.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ДОБАВЛЕН АТРИБУТ "url"
@FeignClient(
        name = "open-library",
        url = "${open.library.api.url}",
        configuration = FeignClientConfig.class
)
public interface OpenLibraryClient {

    @GetMapping("/search.json")
    OpenLibrarySearchResponse searchBooks(
            @RequestParam("q") String query,
            @RequestParam("limit") int limit
    );
}