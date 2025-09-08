package com.example.skillboxsecondtask.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class StudentEventListener {

    @EventListener
    public void onStudentCreated(StudentCreatedEvent event) {
        log.info("СОБЫТИЕ: Студент создан -> {}", event.getStudent());
    }

    @EventListener
    public void onStudentDeleted(StudentDeletedEvent event) {
        log.info("СОБЫТИЕ: Студент удален. ID -> {}", event.getStudentId());
    }
}
