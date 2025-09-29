package com.example.booksManagement.client;

import com.example.booksManagement.client.googlebooks.GoogleBooksSearchResponse;
import com.example.booksManagement.configuration.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "googleBooksClient", url = "https://www.googleapis.com/books/v1", configuration = FeignClientConfig.class)
public interface GoogleBooksClient {

    @GetMapping("/volumes")
    GoogleBooksSearchResponse searchBooks(@RequestParam("q") String query, @RequestParam("maxResults") int maxResults);
}
