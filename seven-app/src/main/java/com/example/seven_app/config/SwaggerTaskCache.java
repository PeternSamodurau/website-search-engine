package com.example.seven_app.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SwaggerTaskCache {

    private final List<String> taskIds = new ArrayList<>();

    public void setTaskIds(List<String> taskIds) {
        this.taskIds.clear();
        this.taskIds.addAll(taskIds);
    }

    public List<String> getTaskIds() {
        // ИСПРАВЛЕННАЯ СТРОКА
        return Collections.unmodifiableList(this.taskIds);
    }
}