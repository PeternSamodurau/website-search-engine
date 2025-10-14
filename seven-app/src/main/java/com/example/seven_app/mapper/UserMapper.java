package com.example.seven_app.mapper;

import com.example.seven_app.dto.response.UserResponseDto;
import com.example.seven_app.dto.request.UserRequestDto;
import com.example.seven_app.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toDto(User user);

    User toUser(UserResponseDto userDto);

    User toUser(UserRequestDto userRequestDto);
}
