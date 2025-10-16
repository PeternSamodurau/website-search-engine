package com.example.seven_app.controller;

import com.example.seven_app.dto.response.TaskResponseDto;
import com.example.seven_app.dto.request.TaskRequestDto;
import com.example.seven_app.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Controller", description = "API для управления задачами")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Получить все задачи", description = "Возвращает полный список всех задач.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задачи успешно получены",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TaskResponseDto.class))))
    })
    @GetMapping
    public Flux<TaskResponseDto> findAll() {
        log.info("Request to find all tasks");
        return taskService.findAll();
    }

    @Operation(summary = "Получить задачу по ID", description = "Возвращает задачу по ее уникальному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно найдена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Задача с таким ID не найдена", content = @Content)
    })
    @GetMapping("/{id}")
    public Mono<TaskResponseDto> findById(@PathVariable String id) {
        log.info("Request to find task by id: {}", id);
        return taskService.findById(id);
    }

    @Operation(summary = "Создать новую задачу", description = "Создает новую задачу.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Задача успешно создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content),
            @ApiResponse(responseCode = "409", description = "Задача с таким именем и описанием уже существует", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TaskResponseDto> save(@RequestBody TaskRequestDto taskRequestDto) {
        log.info("Request to save task: {}", taskRequestDto);
        return taskService.save(taskRequestDto);
    }

    @Operation(summary = "Обновить существующую задачу", description = "Обновляет существующую задачу по ее ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Задача с таким ID не найдена", content = @Content),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content)
    })
    @PutMapping("/{id}")
    public Mono<TaskResponseDto> update(@PathVariable String id, @RequestBody TaskRequestDto taskRequestDto) {
        log.info("Request to update task by id: {} with data: {}", id, taskRequestDto);
        return taskService.update(id, taskRequestDto);
    }

    @Operation(summary = "Удалить задачу", description = "Удаляет задачу по ее ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Задача успешно удалена", content = @Content),
            @ApiResponse(responseCode = "404", description = "Задача с таким ID не найдена", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(@PathVariable String id) {
        log.info("Request to delete task by id: {}", id);
        return taskService.deleteById(id);
    }

    @Operation(summary = "Добавить наблюдателя к задаче", description = "Добавляет пользователя в качестве наблюдателя к существующей задаче.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Наблюдатель успешно добавлен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Задача или пользователь с таким ID не найден", content = @Content),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пользователь уже является наблюдателем)", content = @Content)
    })
    @PostMapping("/{id}/observers/{observerId}")
    public Mono<TaskResponseDto> addObserver(@PathVariable String id, @PathVariable String observerId) {
        log.info("Request to add observer with id: {} to task with id: {}", observerId, id);
        return taskService.addObserver(id, observerId);
    }

    @Operation(summary = "Назначить исполнителя задачи", description = "Назначает пользователя в качестве исполнителя для существующей задачи.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Исполнитель успешно назначен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Задача или пользователь с таким ID не найден", content = @Content),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content)
    })
    @PutMapping("/{id}/assignee/{assigneeId}")
    public Mono<TaskResponseDto> assignAssignee(@PathVariable String id, @PathVariable String assigneeId) {
        log.info("Request to assign assignee with id: {} to task with id: {}", assigneeId, id);
        return taskService.assignAssignee(id, assigneeId);
    }
}
