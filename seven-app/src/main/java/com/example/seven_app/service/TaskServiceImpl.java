package com.example.seven_app.service;

import com.example.seven_app.dto.TaskDto;
import com.example.seven_app.dto.TaskRequestDto;
import com.example.seven_app.mapper.TaskMapper;
import com.example.seven_app.model.Task;
import com.example.seven_app.model.TaskStatus;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.TaskRepository;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Override
    public Flux<TaskDto> findAll() {
        return taskRepository.findAll()
                .flatMap(this::enrichAndMapToDto);
    }

    @Override
    public Mono<TaskDto> findById(String id) {
        return taskRepository.findById(id)
                .flatMap(this::enrichAndMapToDto);
    }

    @Override
    public Mono<TaskDto> save(TaskRequestDto taskDto) {
        // NOTE: The authorId should be set from the security context of the currently logged-in user.
        // This functionality is not implemented here as it requires Spring Security setup.
        Task task = taskMapper.toEntity(taskDto);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        task.setStatus(TaskStatus.TODO);
        if (task.getObserverIds() == null) {
            task.setObserverIds(new HashSet<>());
        }
        return taskRepository.save(task)
                .flatMap(this::enrichAndMapToDto);
    }

    @Override
    public Mono<TaskDto> update(String id, TaskRequestDto taskDto) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    taskMapper.updateTaskFromDto(taskDto, existingTask);
                    existingTask.setUpdatedAt(Instant.now());
                    return taskRepository.save(existingTask);
                })
                .flatMap(this::enrichAndMapToDto);
    }

    @Override
    public Mono<TaskDto> addObserver(String taskId, String observerId) {
        return taskRepository.findById(taskId)
                .flatMap(task -> userRepository.existsById(observerId)
                        .flatMap(exists -> {
                            if (exists) {
                                if (task.getObserverIds() == null) {
                                    task.setObserverIds(new HashSet<>());
                                }
                                task.getObserverIds().add(observerId);
                                task.setUpdatedAt(Instant.now());
                                return taskRepository.save(task);
                            }
                            return Mono.just(task); // If user doesn't exist, do nothing
                        }))
                .flatMap(this::enrichAndMapToDto);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return taskRepository.deleteById(id);
    }

    private Mono<TaskDto> enrichAndMapToDto(Task task) {
        Mono<User> authorMono = userRepository.findById(task.getAuthorId())
                .defaultIfEmpty(new User()); // Should not be empty in a consistent DB

        Mono<User> assigneeMono = Mono.justOrEmpty(task.getAssigneeId())
                .flatMap(userRepository::findById)
                .defaultIfEmpty(new User());

        Mono<Set<User>> observersMono = userRepository.findAllById(
                task.getObserverIds() == null ? Collections.emptySet() : task.getObserverIds()
        ).collect(Collectors.toSet());

        return Mono.zip(authorMono, assigneeMono, observersMono)
                .map(tuple -> taskMapper.toDto(task, tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }
}
