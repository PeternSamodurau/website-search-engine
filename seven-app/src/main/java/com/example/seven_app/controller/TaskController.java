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
import org.springframework.security.core.annotation.AuthenticationPrincipal; // <-- ИМПОРТ
import org.springframework.security.core.userdetails.UserDetails; // <-- ИМПОРТ
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
    public Mono<TaskResponseDto> createTask(@Valid @RequestBody TaskRequestDto request, @AuthenticationPrincipal UserDetails userDetails) { // <-- ИЗМЕНЕНО
        log.info("Request to create task: {} by user {}", request, userDetails.getUsername()); // <-- УЛУЧШЕНО ЛОГИРОВАНИЕ
        return taskService.save(request, userDetails); // <-- ИЗМЕНЕНО
    }

    @Operation(summary = "Обновить существующую задачу", description = "Обновляет существующую задачу по ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<TaskResponseDto> updateTask(@PathVariable String id, @Valid @RequestBody TaskRequestDto request) {
        log.info("Request to update task with id: {}", id);
        return taskService.update(id, request);
    }

    @Operation(summary = "Удалить задачу", description = "Удаляет задачу по ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<Void> deleteTask(@PathVariable String id) {
        log.info("Request to delete task with id: {}", id);
        return taskService.deleteById(id);
    }

    @Operation(summary = "Добавить наблюдателя к задаче", description = "Добавляет существующего пользователя в качестве наблюдателя к задаче")
    @PostMapping("/{id}/observer/{observerId}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public Mono<TaskResponseDto> addObserverToTask(@PathVariable String id, @PathVariable String observerId) {
        log.info("Request to add observer with id: {} to task with id: {}", observerId, id);
        return taskService.addObserver(id, observerId);
    }
}