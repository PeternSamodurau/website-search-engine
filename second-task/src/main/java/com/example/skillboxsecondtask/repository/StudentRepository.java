package com.example.skillboxsecondtask.repository;

import com.example.skillboxsecondtask.model.Student;
import java.util.List;
import java.util.UUID;

public interface StudentRepository {
    List<Student> findAll();
    Student save(Student student);
    void deleteById(UUID id);
    void deleteAll();
}
