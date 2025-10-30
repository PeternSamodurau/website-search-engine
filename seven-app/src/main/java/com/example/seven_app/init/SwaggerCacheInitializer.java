package com.example.seven_app.init;

import com.example.seven_app.config.SwaggerCache;
import com.example.seven_app.repository.TaskRepository;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class SwaggerCacheInitializer {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SwaggerCache swaggerCache;

    @Scheduled(fixedRate = 1000)
    public void cacheSwaggerIds() {

        userRepository.findAll()
                .map(user -> user.getId())
                .collectList()
                .doOnSuccess(userIds -> {
                    if (!swaggerCache.getUserIds().equals(userIds)) {
                        swaggerCache.setUserIds(userIds);
                        log.info("Swagger cache updated with {} user IDs.", userIds.size());
                    }
                })
                .subscribe();

        taskRepository.findAll()
                .map(task -> task.getId())
                .collectList()
                .doOnSuccess(taskIds -> {
                    if (!swaggerCache.getTaskIds().equals(taskIds)) {
                        swaggerCache.setTaskIds(taskIds);
                        log.info("Swagger cache updated with {} task IDs.", taskIds.size());
                    }
                })
                .subscribe();
    }
}
