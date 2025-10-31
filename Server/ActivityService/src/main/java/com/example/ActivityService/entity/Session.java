package com.example.ActivityService.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    private Long userId;

    private SessionType type;

    @Column(name = "prompt",nullable = true)
    private String prompt;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
    public enum SessionType {
        RUNNING,
        CYCLING,
        WALKING
    }
}
