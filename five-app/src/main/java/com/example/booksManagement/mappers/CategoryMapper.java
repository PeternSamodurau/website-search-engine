package com.example.booksManagement.mappers;

import com.example.booksManagement.dto.request.UserCategoryRequest; // ИСПРАВЛЕНО
import com.example.booksManagement.dto.response.CategoryResponse;
import com.example.booksManagement.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    /**
     * Преобразует сущность Category в CategoryResponse DTO.
     * @param category Сущность для преобразования.
     * @return CategoryResponse DTO.
     */
    public CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        return response;
    }

    /**
     * Создает новую сущность Category из UserCategoryRequest DTO.
     * @param request DTO с данными для создания.
     * @return Новая сущность Category.
     */
    public Category toEntity(UserCategoryRequest request) { // ИСПРАВЛЕНО
        Category category = new Category();
        category.setName(request.getName());
        return category;
    }

    /**
     * Обновляет существующую сущность Category данными из UserCategoryRequest DTO.
     * @param request DTO с новыми данными.
     * @param category Существующая сущность для обновления.
     */
    public void updateEntity(UserCategoryRequest request, Category category) { // ИСПРАВЛЕНО
        category.setName(request.getName());
    }
}