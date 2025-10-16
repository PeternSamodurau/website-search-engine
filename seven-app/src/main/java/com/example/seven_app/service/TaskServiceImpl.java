package com.example.seven_app.service;

import com.example.seven_app.dto.request.TaskRequestDto;
import com.example.seven_app.dto.response.TaskResponseDto;
import com.example.seven_app.mapper.TaskMapper;
import com.example.seven_app.model.Task;
import com.example.seven_app.model.TaskStatus;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.TaskRepository;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Value("${app.default-author-username}")
    private String defaultAuthorUsername;

    @Value("${app.default-author-usermail}")
    private String defaultAuthorUsermail;


    @Override
    public Flux<TaskResponseDto> findAll() {
        log.info("Request to find all tasks");
        return taskRepository.findAll()
                .flatMap(this::enrichAndMapToDto)
                .doOnComplete(() -> log.info("Successfully found all tasks"))
                .doOnError(error -> log.error("Error while finding all tasks: {}", error.getMessage()));
    }

    @Override
    public Mono<TaskResponseDto> findById(String id) {
        log.info("Request to find task by id: {}", id);
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task with id " + id + " not found")))
                .flatMap(this::enrichAndMapToDto)
                .doOnSuccess(task -> log.info("Successfully found task: {}", task))
                .doOnError(error -> log.error("Error while finding task by id {}: {}", id, error.getMessage()));
    }

    @Override
    public Mono<TaskResponseDto> save(TaskRequestDto taskRequestDto) {
        log.info("Request to save task: {}", taskRequestDto);
        return taskRepository.existsByNameAndDescription(taskRequestDto.getName(), taskRequestDto.getDescription())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task with the same name and description already exists"));
                    }
                    return userRepository.findByUsernameOrEmail(defaultAuthorUsername, defaultAuthorUsermail)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default author user not found")))
                            .flatMap(author -> {
                                Task task = new Task();
                                task.setAuthorId(author.getId());
                                task.setName(taskRequestDto.getName());
                                task.setDescription(taskRequestDto.getDescription());
                                task.setStatus(TaskStatus.TODO);
                                task.setAssigneeId(taskRequestDto.getAssigneeId());

                                if (taskRequestDto.getObserverIds() != null) {
                                    task.setObserverIds(new HashSet<>(taskRequestDto.getObserverIds()));
                                } else {
                                    task.setObserverIds(new HashSet<>());
                                }
                                task.setCreatedAt(Instant.now());
                                task.setUpdatedAt(Instant.now());

                                return taskRepository.save(task);
                            });
                })
                .flatMap(this::enrichAndMapToDto)
                .doOnSuccess(task -> log.info("Successfully saved task: {}", task))
                .doOnError(error -> log.error("Error while saving task {}: {}", taskRequestDto, error.getMessage()));
    }

    @Override
    public Mono<TaskResponseDto> update(String id, TaskRequestDto taskDto) {
        log.info("Request to update task with id {}: {}", id, taskDto);
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task with id " + id + " not found")))
                .flatMap(existingTask -> {
                    taskMapper.updateTaskFromDto(taskDto, existingTask);
                    existingTask.setUpdatedAt(Instant.now());
                    return taskRepository.save(existingTask);
                })
                .flatMap(this::enrichAndMapToDto)
                .doOnSuccess(task -> log.info("Successfully updated task: {}", task))
                .doOnError(error -> log.error("Error while updating task with id {}: {}", id, error.getMessage()));
    }

    @Override
    public Mono<TaskResponseDto> addObserver(String taskId, String observerId) {
        log.info("Request to add observer {} to task {}", observerId, taskId);
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Задача с ID " + taskId + " не найдена")))
                .flatMap(task -> userRepository.existsById(observerId)
                        .flatMap(userExists -> {
                            if (!userExists) {
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь с ID " + observerId + " не найден"));
                            }

                            if (task.getObserverIds() == null) {
                                task.setObserverIds(new HashSet<>());
                            }

                            if (task.getObserverIds().contains(observerId)) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пользователь уже является наблюдателем"));
                            }

                            task.getObserverIds().add(observerId);
                            task.setUpdatedAt(Instant.now());
                            return taskRepository.save(task);
                        }))
                .flatMap(this::enrichAndMapToDto)
                .doOnSuccess(task -> log.info("Successfully added observer to task: {}", task))
                .doOnError(error -> log.error("Error while adding observer {} to task {}: {}", observerId, taskId, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        log.info("Request to delete task by id: {}", id);
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task with id " + id + " not found")))
                .flatMap(taskRepository::delete)
                .doOnSuccess(v -> log.info("Successfully deleted task with id: {}", id))
                .doOnError(error -> log.error("Error while deleting task with id {}: {}", id, error.getMessage()));
    }

    @Override
    public Mono<TaskResponseDto> assignAssignee(String taskId, String assigneeId) {
        log.info("Request to assign assignee {} to task {}", assigneeId, taskId);
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Задача с ID " + taskId + " не найдена")))
                .flatMap(task -> userRepository.existsById(assigneeId)
                        .flatMap(userExists -> {
                            if (!userExists) {
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь с ID " + assigneeId + " не найден"));
                            }
                            task.setAssigneeId(assigneeId);
                            task.setUpdatedAt(Instant.now());
                            return taskRepository.save(task);
                        }))
                .flatMap(this::enrichAndMapToDto)
                .doOnSuccess(task -> log.info("Successfully assigned assignee to task: {}", task))
                .doOnError(error -> log.error("Error while assigning assignee {} to task {}: {}", assigneeId, taskId, error.getMessage()));
    }

    private Mono<TaskResponseDto> enrichAndMapToDto(Task task) {
        Mono<User> authorMono = Mono.justOrEmpty(task.getAuthorId())
                .flatMap(userRepository::findById)
                .defaultIfEmpty(new User());

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
