package com.example.skillboxsecondtask.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private UUID id;
    private String firstName;
    private String lastName;
    private int age;
}
