package com.example.skillboxsecondtask.shell;

import com.example.skillboxsecondtask.model.Student;
import com.example.skillboxsecondtask.service.StudentServicePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.UUID;
import java.util.stream.Collectors;
@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class StudentCommands {

    private final StudentServicePublisher studentService;

    @ShellMethod(key = "list", value = "List all students")
    public String list() {
        return "List of all students:\n" +
                studentService.getAllStudents().stream()
                        .map(Student::toString)
                        .collect(Collectors.joining("\n"));
    }

    @ShellMethod(key = "add", value = "Add a new student")
    public String add(
            @ShellOption String firstName,
            @ShellOption String lastName,
            @ShellOption int age
    ) {
        Student newStudent = studentService.addStudent(firstName, lastName, age);
        return "Student added successfully: " + newStudent;
    }

    @ShellMethod(key = "delete", value = "Delete a student by ID")
    public String delete(@ShellOption String id) {
        studentService.deleteStudent(UUID.fromString(id));
        return "Student with ID " + id + " deleted successfully.";
    }

    @ShellMethod(key = "purge", value = "Delete all students")
    public String purge() {
        studentService.clearStudents();
        return "All students have been deleted.";
    }
}
