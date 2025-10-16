package com.example.seven_app.init;

import com.example.seven_app.model.Task;
import com.example.seven_app.model.TaskStatus;
import com.example.seven_app.repository.TaskRepository;
import com.example.seven_app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
@Order(3)
@Slf4j
public class TaskInitializer implements CommandLineRunner {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskInitializer(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

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
                    // ИСПОЛЬЗУЕМ ПРАВИЛЬНЫЙ МЕТОД ДЛЯ ПОИСКА
                    return userRepository.findByUsernameOrEmail("user", "user")
                            .switchIfEmpty(Mono.error(new IllegalStateException("Default author 'user' not found.")))
                            .flatMap(author -> {
                                Instant now = Instant.now();

                                // ИСПОЛЬЗУЕМ ПУСТОЙ КОНСТРУКТОР И СЕТТЕРЫ
                                Task task1 = new Task();
                                task1.setName("Task 1 - Buy groceries");
                                task1.setDescription("Milk, Bread, Cheese");
                                task1.setStatus(TaskStatus.TODO);
                                task1.setCreatedAt(now);
                                task1.setUpdatedAt(now);
                                // УСТАНАВЛИВАЕМ ID АВТОРА
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