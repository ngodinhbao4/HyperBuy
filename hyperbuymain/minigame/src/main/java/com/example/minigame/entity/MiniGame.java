package com.example.minigame.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 🎮 Bảng MiniGame
 * Lưu thông tin từng trò chơi như “Vòng quay may mắn”, “Đăng nhập nhận quà”...
 */
@Entity
@Table(name = "MiniGame")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MiniGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String gameId;

    private String name;              // Tên minigame
    private String description;       // Mô tả
    private String rewardType;        // POINTS | VOUCHER
    private String rewardValue;       // Giá trị thưởng (số điểm hoặc mã voucher)
    private String status;            // ACTIVE / INACTIVE

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    public enum GameStatus {
        ACTIVE,
        INACTIVE,
        ENDED
    }
}
