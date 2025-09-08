package com.example.skillboxsecondtask.init;

import com.example.skillboxsecondtask.service.StudentServicePublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Profile("init")
@RequiredArgsConstructor
public class StudentInitializer {

    private final StudentServicePublisher studentService;

    @Value("${app.init.students-file-path:default-students.txt}")
    private String studentsFilePath;

    @PostConstruct
    public void initialize() {
        log.info("Profile 'init' is active. Initializing students from file: {}", studentsFilePath);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(studentsFilePath)) {
            if (is == null) {
                log.error("ERROR: File '{}' not found in classpath.", studentsFilePath);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                long count = reader.lines()
                        .map(line -> line.split(","))
                        .filter(parts -> parts.length == 3)
                        .peek(parts -> {
                            try {
                                studentService.addStudent(parts[0], parts[1], Integer.parseInt(parts[2].trim()));
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse age in line: '{}'", String.join(",", parts));
                            }
                        })
                        .count();
                log.info("Successfully loaded {} students.", count);
            }
        } catch (Exception e) {
            log.error("An error occurred while loading default students: {}", e.getMessage(), e);
        }
    }
}
