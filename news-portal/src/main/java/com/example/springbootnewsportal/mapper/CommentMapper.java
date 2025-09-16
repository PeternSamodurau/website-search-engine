package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;
import com.example.springbootnewsportal.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    // Правильный маппинг: из поля '''author''' берем '''username''' и кладем в '''authorUsername'''
    @Mapping(source = "author.username", target = "authorUsername")
    CommentResponse toCommentResponse(Comment comment);

    // Игнорируем поле '''author''', а не '''user'''
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "news", ignore = true)
    Comment toComment(CommentRequest request);

    @Mapping(target = "id", ignore = true)
    // Игнорируем поле '''author''', а не '''user'''
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "news", ignore = true)
    void updateCommentFromRequest(CommentRequest request, @MappingTarget Comment comment);
}
