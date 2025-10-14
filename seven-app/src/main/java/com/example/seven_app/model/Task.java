package com.example.seven_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    private String name;
    private String description;

    //состояние задачи
    private TaskStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    //связи с пользователями хранятся в базе данных
    private String authorId;
    private String assigneeId;
    private Set<String> observerIds;

    //объекты не храняться в базе данных, только чтение
    @ReadOnlyProperty
    private User author;

    @ReadOnlyProperty
    private User assignee;

    @ReadOnlyProperty
    private Set<User> observers;
}
