package com.example.seven_app.init;

import com.example.seven_app.model.Task;
import com.example.seven_app.model.TaskStatus;
import com.example.seven_app.repository.TaskRepository;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("init")
@Slf4j
@Order(3)
public class TaskInitializer implements CommandLineRunner {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Value("${app.default-author-username}")
    private String defaultAuthorUsername;

    @Value("${app.default-author-usermail}")
    private String defaultAuthorUsermail;

    @Override
    public void run(String... args) {
        log.info(">>>>>> TaskInitializer: Checking for existing tasks... <<<<<<<");

        taskRepository.count()
            .flatMap(count -> {
                if (count > 0) {
                    log.info("Tasks already exist. Skipping creation.");
                    return Mono.empty();
                }

                log.info("No tasks found. Creating 3 test tasks...");

                // Ищем автора по имени и email из properties, как в AuthorInitializer
                return userRepository.findByUsernameOrEmail(defaultAuthorUsername, defaultAuthorUsermail)
                    .switchIfEmpty(Mono.error(new IllegalStateException("Default author '" + defaultAuthorUsername + "' not found. TaskInitializer must run AFTER AuthorInitializer.")))
                    .flatMap(author -> {
                        Instant now = Instant.now();

                        Task task1 = new Task();
                        task1.setName("Task 1 - Buy groceries");
                        task1.setDescription("Milk, Bread, Cheese");
                        task1.setStatus(TaskStatus.TODO);
                        task1.setCreatedAt(now);
                        task1.setUpdatedAt(now);
                        task1.setAuthorId(author.getId());

                        Task task2 = new Task();
                        task2.setName("Task 2 - Fix the car");
                        task2.setDescription("Check the engine light");
                        task2.setStatus(TaskStatus.IN_PROGRESS);
                        task2.setCreatedAt(now);
                        task2.setUpdatedAt(now);
                        task2.setAuthorId(author.getId());

                        Task task3 = new Task();
                        task3.setName("Task 3 - Deploy to production");
                        task3.setDescription("Release version 1.2.0");
                        task3.setStatus(TaskStatus.DONE);
                        task3.setCreatedAt(now);
                        task3.setUpdatedAt(now);
                        task3.setAuthorId(author.getId());

                        return taskRepository.saveAll(List.of(task1, task2, task3))
                                .then();
                    });
            })
            .block();

        log.info(">>>>>> TaskInitializer: FINISHED. <<<<<<<");
    }
}
