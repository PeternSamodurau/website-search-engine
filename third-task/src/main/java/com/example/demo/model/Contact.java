package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("contacts")
@FieldNameConstants
public class Contact {

    @Id
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}