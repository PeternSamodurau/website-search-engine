package com.example.booksManagement.model;

import jakarta.persistence.*;
import lombok.*; // <- ИЗМЕНЕН ИМПОРТ

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "category")
    @ToString.Exclude
    @Builder.Default
    private List<Book> books = new ArrayList<>();
}