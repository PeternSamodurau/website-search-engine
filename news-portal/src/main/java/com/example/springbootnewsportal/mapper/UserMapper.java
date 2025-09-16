package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.UserRequest;
import com.example.springbootnewsportal.dto.response.UserResponse;
import com.example.springbootnewsportal.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    User toUser(UserRequest request);

    UserResponse toUserResponse(User user);

    void updateUserFromRequest(UserRequest request, @MappingTarget User user);
}
