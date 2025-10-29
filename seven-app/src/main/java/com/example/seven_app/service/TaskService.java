package com.example.seven_app.service;

import com.example.seven_app.dto.response.TaskResponseDto;
import com.example.seven_app.dto.request.TaskRequestDto;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskService {
    Flux<TaskResponseDto> findAll();

    Mono<TaskResponseDto> findById(String id);

    Mono<TaskResponseDto> save(TaskRequestDto taskDto, UserDetails userDetails);

    Mono<TaskResponseDto> update(String id, TaskRequestDto taskDto, UserDetails userDetails);

    Mono<TaskResponseDto> addObserver(String taskId, String observerId, UserDetails userDetails);

    Mono<Void> deleteById(String id, UserDetails userDetails);

    Mono<TaskResponseDto> assignAssignee(String taskId, String assigneeId, UserDetails userDetails);
}
