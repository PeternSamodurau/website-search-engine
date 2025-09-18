package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import com.example.springbootnewsportal.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Категории", description = "Операции для создания, получения, обновления и удаления категорий новостей.")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Получить все категории", description = "Возвращает постраничный список всех категорий новостей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список категорий успешно получен",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)) })
    })
    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(Pageable pageable) {
        return ResponseEntity.ok(categoryService.findAll(pageable));
    }

    @Operation(summary = "Получить категорию по ID", description = "Возвращает одну категорию по ее уникальному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория найдена",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)) }),
            @ApiResponse(responseCode = "404", description = "Категория с таким ID не найдена", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "Уникальный идентификатор категории") @PathVariable Long id
    ) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @Operation(summary = "Создать новую категорию", description = "Создает новую категорию новостей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Категория успешно создана",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody(description = "Название новой категории") @Valid @org.springframework.web.bind.annotation.RequestBody CategoryRequest request
    ) {
        CategoryResponse createdCategory = categoryService.create(request);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить категорию", description = "Обновляет название существующей категории по ее ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория успешно обновлена",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе", content = @Content),
            @ApiResponse(responseCode = "404", description = "Категория с таким ID не найдена", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @Parameter(description = "ID категории, которую нужно обновить") @PathVariable Long id,
            @RequestBody(description = "Новое название для категории") @Valid @org.springframework.web.bind.annotation.RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @Operation(summary = "Удалить категорию", description = "Удаляет категорию по ее ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Категория успешно удалена", content = @Content),
            @ApiResponse(responseCode = "404", description = "Категория с таким ID не найдена", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID категории, которую нужно удалить") @PathVariable Long id
    ) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
