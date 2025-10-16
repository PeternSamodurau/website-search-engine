package com.example.seven_app.config;

import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("init")
@Slf4j
@Order(3) // Lowest priority
public class SwaggerCacheInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SwaggerUserCache swaggerUserCache;

    @Override
    public void run(String... args) {
        log.info(">>>>>>>>> Caching user IDs for Swagger... <<<<<<<<<");
        try {
            List<String> userIds = userRepository.findAll()
                    .map(User::getId)
                    .collectList()
                    .block(); // This is safe and necessary on startup

            if (userIds == null) {
                userIds = Collections.emptyList();
            }

            swaggerUserCache.setUserIds(userIds);
            log.info("Successfully cached {} user IDs for Swagger.", userIds.size());
        } catch (Exception e) {
            log.error("Failed to cache user IDs for Swagger", e);
            swaggerUserCache.setUserIds(Collections.emptyList());
        }
        log.info(">>>>>>>>> SwaggerCacheInitializer FINISHED. <<<<<<<<<");
    }
}
