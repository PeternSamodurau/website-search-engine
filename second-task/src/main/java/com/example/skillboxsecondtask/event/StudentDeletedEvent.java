package com.example.skillboxsecondtask.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Slf4j
public class StudentDeletedEvent extends ApplicationEvent {

    private final UUID studentId;

    public StudentDeletedEvent(Object source, UUID studentId) {
        super(source);
        this.studentId = studentId;
        log.info("КОНТЕЙНЕР: Создан объект события StudentDeletedEvent для ID: {}", studentId);
    }

    public UUID getStudentId() {
        return studentId;
    }
}
