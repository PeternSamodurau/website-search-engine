package com.example.skillboxsecondtask.service;

import com.example.skillboxsecondtask.event.StudentCreatedEvent;
import com.example.skillboxsecondtask.event.StudentDeletedEvent;
import com.example.skillboxsecondtask.model.Student;
import com.example.skillboxsecondtask.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class StudentServicePublisherImpl implements StudentServicePublisher {

    private final StudentRepository repository;
    private final ApplicationEventPublisher publisher;

    public StudentServicePublisherImpl(StudentRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public List<Student> getAllStudents() {
        log.info("Запрошен список всех студентов");
        return repository.findAll();
    }

    @Override
    public Student addStudent(String firstName, String lastName, int age) {
        log.info("Попытка добавления нового студента: {} {}", firstName, lastName);
        Student studentToSave = new Student(null, firstName, lastName, age);
        Student savedStudent = repository.save(studentToSave);
        log.info("Студент успешно сохранен с id: {}", savedStudent.getId());

        publisher.publishEvent(new StudentCreatedEvent(this, savedStudent));
        log.info("Опубликовано событие StudentCreatedEvent для студента с id: {}", savedStudent.getId());
        return savedStudent;
    }

    @Override
    public void deleteStudent(UUID id) {
        log.info("Попытка удаления студента с id: {}", id);
        repository.deleteById(id);
        log.info("Студент с id: {} успешно удален из репозитория", id);

        publisher.publishEvent(new StudentDeletedEvent(this, id));
        log.info("Опубликовано событие StudentDeletedEvent для студента с id: {}", id);
    }

    @Override
    public void clearStudents() {
        log.warn("Попытка удаления ВСЕХ студентов");
        repository.deleteAll();
        log.info("Все студенты были удалены");
    }
}
