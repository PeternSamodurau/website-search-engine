package com.example.seven_app.controller;

import com.example.seven_app.dto.request.TaskRequestDto;
import com.example.seven_app.dto.response.TaskResponseDto;
import com.example.seven_app.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Controller", description = "Операции с задачами")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Получить все задачи", description = "Возвращает список всех задач")
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Flux<TaskResponseDto> getAllTasks() {
        log.info("Request to get all tasks");
        return taskService.findAll();
    }

    @Operation(summary = "Получить задачу по ID", description = "Возвращает одну задачу по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<TaskResponseDto> getTaskById(@PathVariable String id) {
        log.info("Request to get task by id: {}", id);
        return taskService.findById(id);
    }

    @Operation(summary = "Создать новую задачу", description = "Создает новую задачу")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<TaskResponseDto> createTask(@Valid @RequestBody TaskRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to create task: {} by user {}", request, userDetails.getUsername());
        return taskService.save(request, userDetails);
    }

    @Operation(summary = "Обновить существующую задачу", description = "Обновляет существующую задачу по ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<TaskResponseDto> updateTask(@PathVariable String id, @Valid @RequestBody TaskRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to update task with id: {} by user {}", id, userDetails.getUsername());
        return taskService.update(id, request, userDetails);
    }

    @Operation(summary = "Удалить задачу", description = "Удаляет задачу по ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<Void> deleteTask(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to delete task with id: {} by user {}", id, userDetails.getUsername());
        return taskService.deleteById(id, userDetails);
    }

    @Operation(summary = "Добавить наблюдателя к задаче", description = "Добавляет существующего пользователя в качестве наблюдателя к задаче")
    @PostMapping("/{id}/observer/{observerId}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<TaskResponseDto> addObserverToTask(@PathVariable String id, @PathVariable String observerId, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to add observer with id: {} to task with id: {} by user {}", observerId, id, userDetails.getUsername());
        return taskService.addObserver(id, observerId, userDetails);
    }

    @Operation(summary = "Назначить исполнителя для задачи", description = "Назначает существующего пользователя в качестве исполнителя для задачи")
    @PostMapping("/{id}/assignee/{assigneeId}")
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<TaskResponseDto> assignAssignee(@PathVariable String id, @PathVariable String assigneeId, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Request to assign assignee with id: {} to task with id: {} by user {}", assigneeId, id, userDetails.getUsername());
        return taskService.assignAssignee(id, assigneeId, userDetails);
    }
}