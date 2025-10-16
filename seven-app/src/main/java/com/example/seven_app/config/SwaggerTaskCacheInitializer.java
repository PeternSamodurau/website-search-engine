package com.example.seven_app.config;

import com.example.seven_app.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Order(11) // Runs after all data initializers
public class SwaggerTaskCacheInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SwaggerTaskCacheInitializer.class);

    private final TaskRepository taskRepository;
    private final SwaggerTaskCache swaggerTaskCache;

    public SwaggerTaskCacheInitializer(TaskRepository taskRepository, SwaggerTaskCache swaggerTaskCache) {
        this.taskRepository = taskRepository;
        this.swaggerTaskCache = swaggerTaskCache;
    }

    @Override
    public void run(String... args) {
        log.info(">>>>>>> SwaggerTaskCacheInitializer: Caching task IDs for Swagger... <<<<<<<");

        taskRepository.findAll()
                .map(com.example.seven_app.model.Task::getId)
                .collect(Collectors.toList())
                .doOnSuccess(taskIds -> {
                    swaggerTaskCache.setTaskIds(taskIds);
                    log.info("Successfully cached {} task IDs for Swagger.", taskIds.size());
                })
                .block(); // Safe to block here during startup initialization

        log.info(">>>>>>> SwaggerTaskCacheInitializer: FINISHED. <<<<<<<");
    }
}
