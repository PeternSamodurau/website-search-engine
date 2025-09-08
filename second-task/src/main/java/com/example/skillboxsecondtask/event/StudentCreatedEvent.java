package com.example.skillboxsecondtask.event;

import com.example.skillboxsecondtask.model.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
public class StudentCreatedEvent extends ApplicationEvent {

    private final Student student;

    public StudentCreatedEvent(Object source, Student student) {
        super(source);
        this.student = student;
        log.info("КОНТЕЙНЕР: Создан объект события StudentCreatedEvent для студента: {}", student.getFirstName());
    }

    public Student getStudent() {
        return student;
    }
}
