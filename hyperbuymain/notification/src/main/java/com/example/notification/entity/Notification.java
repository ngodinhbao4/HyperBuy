package com.example.notification.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "order_id")
    private String orderId; // Có thể null cho thông báo admin

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String type; // ORDER, ADMIN

    @Column(nullable = false)
    private String status; // SENT, FAILED

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}