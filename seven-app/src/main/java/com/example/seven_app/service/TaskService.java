package com.example.seven_app.service;

import com.example.seven_app.dto.TaskDto;
import com.example.seven_app.dto.TaskRequestDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskService {
    Flux<TaskDto> findAll();

    Mono<TaskDto> findById(String id);

    Mono<TaskDto> save(TaskRequestDto taskDto);

    Mono<TaskDto> update(String id, TaskRequestDto taskDto);

    Mono<TaskDto> addObserver(String taskId, String observerId);

    Mono<Void> deleteById(String id);
}
