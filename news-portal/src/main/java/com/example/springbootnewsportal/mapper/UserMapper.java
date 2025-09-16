package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.UserRequest;
import com.example.springbootnewsportal.dto.response.UserResponse;
import com.example.springbootnewsportal.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "newsCount", expression = "java(user.getNewsList() != null ? (long) user.getNewsList().size() : 0L)")
    @Mapping(target = "commentsCount", expression = "java(user.getComments() != null ? (long) user.getComments().size() : 0L)")
    UserResponse toResponse(User user);

    User toUser(UserRequest request);

    @Mapping(target = "id", ignore = true)
    void updateUserFromRequest(UserRequest request, @MappingTarget User user);
}
