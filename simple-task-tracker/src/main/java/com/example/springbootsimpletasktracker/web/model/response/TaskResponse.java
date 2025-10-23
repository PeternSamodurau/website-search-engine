package com.example.springbootsimpletasktracker.web.model.response;

import com.example.springbootsimpletasktracker.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private String id;

    private String name;

    private String description;

    private TaskStatus status;

    private UserResponse author;

    private UserResponse assignee;

    private List<UserResponse> observers = new ArrayList<>();

}
