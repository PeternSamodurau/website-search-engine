package com.example.seven_app.dto;

import com.example.seven_app.model.TaskStatus;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class TaskDto {
    private String id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private TaskStatus status;
    private UserDto author;
    private UserDto assignee;
    private Set<UserDto> observers;
}
