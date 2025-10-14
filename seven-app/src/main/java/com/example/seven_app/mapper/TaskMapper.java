package com.example.seven_app.mapper;

import com.example.seven_app.dto.response.TaskResponseDto;
import com.example.seven_app.dto.request.TaskRequestDto;
import com.example.seven_app.model.Task;
import com.example.seven_app.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface TaskMapper {

    @Mapping(source = "task.id", target = "id")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "assignee", source = "assignee")
    @Mapping(target = "observers", source = "observers")
    TaskResponseDto toDto(Task task, User author, User assignee, Set<User> observers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "observers", ignore = true)
    Task toEntity(TaskRequestDto requestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "observers", ignore = true)
    void updateTaskFromDto(TaskRequestDto requestDto, @MappingTarget Task task);
}
