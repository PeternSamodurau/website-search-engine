package com.example.seven_app.init;

import com.example.seven_app.config.SwaggerCache;
import com.example.seven_app.repository.TaskRepository;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
@Profile("init")
@Order(4)
public class SwaggerCacheInitializer {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SwaggerCache swaggerCache;

    @EventListener(ApplicationReadyEvent.class)
    public void cacheSwaggerIds() {

        log.info(">>>>>> Caching user IDs for Swagger... <<<<<<<");
        userRepository.findAll()
                .map(user -> user.getId())
                .collectList()
                .doOnSuccess(userIds -> {
                    swaggerCache.setUserIds(userIds);
                    log.info("Successfully cached {} user IDs for Swagger.", userIds.size());
                })
                .block();

        // --- Кэширование задач (новая логика) ---
        log.info(">>>>>> Caching task IDs for Swagger... <<<<<<<");
        taskRepository.findAll()
                .map(task -> task.getId())
                .collectList()
                .doOnSuccess(taskIds -> {
                    swaggerCache.setTaskIds(taskIds);
                    log.info("Successfully cached {} task IDs for Swagger.", taskIds.size());
                })
                .block();

        log.info(">>>>>> SwaggerCacheInitializer: FINISHED. <<<<<<<");
    }
}
