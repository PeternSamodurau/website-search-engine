package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.model.Category;
import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    // Считаем кол-во новостей в категории для ответа
    @Mapping(target = "newsCount", expression = "java(category.getNewsList() != null ? (long) category.getNewsList().size() : 0L)")
    CategoryResponse toResponse(Category category);

    Category toCategory(CategoryRequest request);

    // При обновлении запрещаем менять ID
    @Mapping(target = "id", ignore = true)
    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget Category category);
}
