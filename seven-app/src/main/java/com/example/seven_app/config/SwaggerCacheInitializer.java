package com.example.seven_app.config;

import com.example.seven_app.repository.TaskRepository; // Добавили репозиторий задач
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("init")
public class SwaggerCacheInitializer {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository; // Добавили репозиторий задач
    private final SwaggerCache swaggerCache; // Используем новый единый кэш

    @EventListener(ApplicationReadyEvent.class)
    public void cacheSwaggerIds() {
        // --- Кэширование пользователей (логика без изменений) ---
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