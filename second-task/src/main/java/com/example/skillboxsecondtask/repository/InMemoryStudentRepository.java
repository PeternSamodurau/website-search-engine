package com.example.skillboxsecondtask.repository;

import com.example.skillboxsecondtask.model.Student;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryStudentRepository implements StudentRepository {

    private final Map<UUID, Student> studentMap = new ConcurrentHashMap<>();

    @Override
    public Student save(Student student) {
        if (student.getId() == null) {
            student.setId(UUID.randomUUID());
        }
        studentMap.put(student.getId(), student);
        return student;
    }

    @Override
    public List<Student> findAll() {
        return List.copyOf(studentMap.values());
    }

    @Override
    public void deleteById(UUID id) {
        studentMap.remove(id);
    }

    @Override
    public void deleteAll() {
        studentMap.clear();
    }
}
