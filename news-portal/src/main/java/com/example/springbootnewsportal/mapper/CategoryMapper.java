package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.CategoryRequest;
import com.example.springbootnewsportal.dto.response.CategoryResponse;
import com.example.springbootnewsportal.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    Category toCategory(CategoryRequest request);

    CategoryResponse toCategoryResponse(Category category);

    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget Category category);
}
