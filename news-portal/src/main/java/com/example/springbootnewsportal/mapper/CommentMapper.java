package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.CommentUpdateRequest;
import com.example.springbootnewsportal.model.Comment;
import com.example.springbootnewsportal.dto.request.CommentRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(source = "author.username", target = "authorUsername")
    CommentResponse toResponse(Comment comment);

    List<CommentResponse> toResponseList(List<Comment> comments);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "news", ignore = true)
    Comment toComment(CommentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "news", ignore = true)
    void updateCommentFromRequest(CommentUpdateRequest request, @MappingTarget Comment comment);

}
