package com.example.skillboxsixapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@SpringBootApplication
@EnableKafka
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        String url = "http://localhost:" + port;
        log.info("=================== OrderServiceApplication started on URL : {}", url);
    }

}
