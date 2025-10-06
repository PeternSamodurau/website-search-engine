package com.example.skillboxsixapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
public class SkillboxSixAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillboxSixAppApplication.class, args);
    }
    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        String url = "http://localhost:" + port;
        log.info("=================== SkillboxSixAppApplication started on URL : {}", url);
    }

}
