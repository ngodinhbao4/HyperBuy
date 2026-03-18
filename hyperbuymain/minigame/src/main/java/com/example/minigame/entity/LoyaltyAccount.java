package com.example.minigame.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 💰 Quản lý điểm thưởng của người dùng (dùng để đổi voucher)
 */
@Entity
@Table(name = "LoyaltyAccount")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoyaltyAccount {

    @Id
    private String userId;          // Trùng với ID người dùng từ user-service

    private Integer points;         // Tổng điểm hiện có

    private LocalDateTime updatedAt;  // Lần cuối cập nhật
}
