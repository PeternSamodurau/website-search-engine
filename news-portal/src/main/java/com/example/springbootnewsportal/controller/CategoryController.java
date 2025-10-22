package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import com.example.springbootnewsportal.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Категории", description = "Операции с категориями новостей")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Получить все категории", description = "Возвращает полный список всех категорий новостей без пагинации.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категории успешно получены",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("Request to get all categories");

        List<CategoryResponse> categories = categoryService.findAll();

        log.info("Successfully retrieved {} categories. Response code: 200", categories.size());
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Получить категорию по ID", description = "Возвращает категорию по ее уникальному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория успешно найдена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Категория с таким ID не найдена", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        log.info("Request to get category with id: {}", id);

        CategoryResponse category = categoryService.findById(id);

        log.info("Successfully retrieved category with id: {}. Response code: 200", id);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Создать новую категорию", description = "Создает новую категорию новостей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Категория успешно создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, дубликат имени)", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("Request to create a new category with name: {}", request.getCategoryName());

        CategoryResponse createdCategory = categoryService.create(request);

        log.info("Successfully created a new category with id: {}. Response code: 201", createdCategory.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @Operation(summary = "Обновить существующую категорию", description = "Обновляет имя существующей категории.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Категория с таким ID не найдена", content = @Content),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, дубликат имени)", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        log.info("Request to update category with id: {}", id);

        CategoryResponse updatedCategory = categoryService.update(id, request);

        log.info("Successfully updated category with id: {}. Response code: 200", id);
        return ResponseEntity.ok(updatedCategory);
    }

    @Operation(summary = "Удалить категорию", description = "Удаляет категорию по ее ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Категория успешно удалена", content = @Content),
            @ApiResponse(responseCode = "404", description = "Категория с таким ID не найдена", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("Request to delete category with id: {}", id);

        categoryService.deleteById(id);

        log.info("Successfully deleted category with id: {}. Response code: 204", id);
        return ResponseEntity.noContent().build();
    }
}
