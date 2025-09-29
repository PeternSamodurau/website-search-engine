package com.example.booksManagement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableCaching
@EnableFeignClients
@Slf4j
public class BooksManagement {

    public static void main(String[] args) {
        log.info("My ClassLoader for load classes to Metaspace: {}", BooksManagement.class.getClassLoader());
        SpringApplication.run(BooksManagement.class, args);
    }
    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        String url = "http://localhost:" + port;
        log.info("=================== BooksManagement started on URL : {}", url);
    }

}
