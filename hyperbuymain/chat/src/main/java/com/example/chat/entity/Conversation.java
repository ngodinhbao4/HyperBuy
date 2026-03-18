package com.example.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant1_id")
    private String participant1Id;

    @Column(name = "participant2_id")
    private String participant2Id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}