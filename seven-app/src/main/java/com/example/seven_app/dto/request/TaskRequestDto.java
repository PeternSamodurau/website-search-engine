package com.example.seven_app.dto.request;

import com.example.seven_app.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDto {
    private String name;
    private String description;
    private TaskStatus status;
    private String assigneeId;
    private Set<String> observerIds;
}
