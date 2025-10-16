package com.example.seven_app.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SwaggerCache {

    private final List<String> userIds = new ArrayList<>();
    private final List<String> taskIds = new ArrayList<>(); // Добавили кэш для задач

    // --- Методы для пользователей (без изменений) ---
    public void setUserIds(List<String> userIds) {
        this.userIds.clear();
        this.userIds.addAll(userIds);
    }

    public List<String> getUserIds() {
        return Collections.unmodifiableList(this.userIds);
    }

    // --- Методы для задач (новые) ---
    public void setTaskIds(List<String> taskIds) {
        this.taskIds.clear();
        this.taskIds.addAll(taskIds);
    }

    public List<String> getTaskIds() {
        return Collections.unmodifiableList(this.taskIds);
    }
}