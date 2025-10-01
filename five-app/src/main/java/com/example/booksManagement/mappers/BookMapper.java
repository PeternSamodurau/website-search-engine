package com.example.booksManagement.mappers;

import com.example.booksManagement.dto.request.UserBookRequest;
import com.example.booksManagement.dto.response.BookResponse;
import com.example.booksManagement.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(source = "category.name", target = "categoryName")
    BookResponse toResponse(Book book);

    @Mapping(source = "categoryName", target = "category.name")
    @Mapping(target = "id", ignore = true)
    Book toEntity(UserBookRequest request);
}