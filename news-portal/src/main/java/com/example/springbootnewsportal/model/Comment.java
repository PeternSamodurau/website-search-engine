package com.example.springbootnewsportal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // <--- ДОБАВЛЕН ИМПОРТ

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", nullable = false)
    private String text;

    // === БЛОК ИЗМЕНЕНИЙ НАЧАЛО ===
    @CreationTimestamp // <--- ДОБАВЛЕНА АННОТАЦИЯ
    @Column(name = "created_at", nullable = false, updatable = false) // Сделано неизменяемым
    private Instant createdAt;
    // === БЛОК ИЗМЕНЕНИЙ КОНЕЦ ===

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "news_id", nullable = false)
    private News news;
}
