package com.example.seven_app.controller;

import com.example.seven_app.dto.TaskDto;
import com.example.seven_app.dto.TaskRequestDto;
import com.example.seven_app.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // <-- Добавлен импорт
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j // <-- Добавлена аннотация
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public Flux<TaskDto> findAll() {
        log.info("Request to find all tasks");
        return taskService.findAll()
                .doOnError(error -> log.error("Error finding all tasks", error));
    }

    @GetMapping("/{id}")
    public Mono<TaskDto> findById(@PathVariable String id) {
        log.info("Request to find task by id: {}", id);
        return taskService.findById(id)
                .doOnError(error -> log.error("Error finding task by id: {}", id, error));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TaskDto> save(@RequestBody TaskRequestDto taskRequestDto) {
        log.info("Request to save task: {}", taskRequestDto);
        return taskService.save(taskRequestDto)
                .doOnError(error -> log.error("Error saving task: {}", taskRequestDto, error));
    }

    @PutMapping("/{id}")
    public Mono<TaskDto> update(@PathVariable String id, @RequestBody TaskRequestDto taskRequestDto) {
        log.info("Request to update task by id: {} with data: {}", id, taskRequestDto);
        return taskService.update(id, taskRequestDto)
                .doOnError(error -> log.error("Error updating task by id: {}", id, error));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(@PathVariable String id) {
        log.info("Request to delete task by id: {}", id);
        return taskService.deleteById(id)
                .doOnError(error -> log.error("Error deleting task by id: {}", id, error));
    }

    @PostMapping("/{id}/observers")
    public Mono<TaskDto> addObserver(@PathVariable String id, @RequestBody String observerId) {
        log.info("Request to add observer with id: {} to task with id: {}", observerId, id);
        return taskService.addObserver(id, observerId)
                .doOnError(error -> log.error("Error adding observer to task with id: {}", id, error));
    }
}
