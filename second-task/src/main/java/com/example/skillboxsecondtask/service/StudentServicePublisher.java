package com.example.skillboxsecondtask.service;

import com.example.skillboxsecondtask.model.Student;

import java.util.List;
import java.util.UUID;

public interface StudentServicePublisher {
    List<Student> getAllStudents();
    Student addStudent(String firstName, String lastName, int age);
    void deleteStudent(UUID id);
    void clearStudents();
}