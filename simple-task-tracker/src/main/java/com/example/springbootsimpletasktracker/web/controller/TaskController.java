package com.example.springbootsimpletasktracker.web.controller;

import com.example.springbootsimpletasktracker.entity.Task;
import com.example.springbootsimpletasktracker.mapper.TaskMapper;
import com.example.springbootsimpletasktracker.security.AppUserPrincipal;
import com.example.springbootsimpletasktracker.service.TaskService;
import com.example.springbootsimpletasktracker.web.model.request.UpsertTaskRequest;
import com.example.springbootsimpletasktracker.web.model.response.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    private final TaskMapper taskMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER')")
    public Flux<TaskResponse> findAll() {
        return taskService.findAll().map(taskMapper::taskToResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER')")
    public Mono<ResponseEntity<TaskResponse>> findById(@PathVariable String id) {
        return taskService.findById(id)
                .map(it -> ResponseEntity.ok(taskMapper.taskToResponse(it)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public Mono<ResponseEntity<TaskResponse>> createTask(@RequestBody UpsertTaskRequest request,
                                                         @AuthenticationPrincipal AppUserPrincipal userPrincipal) {
        Mono<Task> newTask = taskService.save(taskMapper.requestToTask(request), userPrincipal.getId());

        return newTask.map(it -> ResponseEntity.status(HttpStatus.CREATED).body(taskMapper.taskToResponse(it)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public Mono<ResponseEntity<TaskResponse>> updateTask(@RequestBody UpsertTaskRequest request,
                                                         @PathVariable String id) {
        Mono<Task> updatedTask = taskService.update(taskMapper.requestToTask(request), id);

        return updatedTask.map(it -> ResponseEntity.ok(taskMapper.taskToResponse(it)));
    }

    @PutMapping("/{id}/observer/{observerId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER')")
    public Mono<ResponseEntity<TaskResponse>> addObserver(@PathVariable String id, @PathVariable String observerId) {
        Mono<Task> updatedTask = taskService.addObserver(id, observerId);

        return updatedTask.map(it -> ResponseEntity.ok(taskMapper.taskToResponse(it)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTask(@PathVariable String id) {
        return taskService.deleteById(id);
    }

}
