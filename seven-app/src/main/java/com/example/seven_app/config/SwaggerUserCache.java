package com.example.seven_app.config;

import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@Getter
public class SwaggerUserCache implements ApplicationListener<ApplicationReadyEvent> {

    private final UserRepository userRepository;
    private List<String> userIds = Collections.emptyList();

    public SwaggerUserCache(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Application is ready. Caching user IDs for Swagger...");
        try {
            // This blocking call is safe here because it runs once at startup,
            // not on a reactive Netty thread.
            this.userIds = userRepository.findAll()
                    .map(User::getId)
                    .collectList()
                    .block();

            if (this.userIds == null) {
                this.userIds = Collections.emptyList();
            }
            log.info("Successfully cached {} user IDs for Swagger.", this.userIds.size());
        } catch (Exception e) {
            log.error("Failed to cache user IDs for Swagger", e);
            this.userIds = Collections.emptyList();
        }
    }
}
