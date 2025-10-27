package com.example.seven_app.service;

import com.example.seven_app.dto.response.TaskResponseDto;
import com.example.seven_app.dto.request.TaskRequestDto;
import org.springframework.security.core.userdetails.UserDetails; // <-- ИМПОРТ
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskService {
    Flux<TaskResponseDto> findAll();

    Mono<TaskResponseDto> findById(String id);

    Mono<TaskResponseDto> save(TaskRequestDto taskDto, UserDetails userDetails); // <-- ИЗМЕНЕНО

    Mono<TaskResponseDto> update(String id, TaskRequestDto taskDto);

    Mono<TaskResponseDto> addObserver(String taskId, String observerId);

    Mono<Void> deleteById(String id);

    Mono<TaskResponseDto> assignAssignee(String taskId, String assigneeId);
}
