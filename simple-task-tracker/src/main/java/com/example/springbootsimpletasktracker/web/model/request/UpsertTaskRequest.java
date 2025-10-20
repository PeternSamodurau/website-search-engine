package com.example.springbootsimpletasktracker.web.model.request;

import com.example.springbootsimpletasktracker.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpsertTaskRequest {

    private String id;

    private String name;

    private String description;

    private TaskStatus status;

    private String authorId;

    private String assigneeId;

    private List<String> observerIds;

}
