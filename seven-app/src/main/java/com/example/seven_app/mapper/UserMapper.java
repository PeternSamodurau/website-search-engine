package com.example.seven_app.mapper;

import com.example.seven_app.dto.UserDto;
import com.example.seven_app.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // Говорим, что это маппер для Spring
public interface UserMapper {

    // "Чертеж" для перевода из User в UserDto
    UserDto toDto(User user);

    // "Чертеж" для перевода из UserDto обратно в User
    User toUser(UserDto userDto);
}