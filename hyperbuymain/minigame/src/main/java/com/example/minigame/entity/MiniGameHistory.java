package com.example.minigame.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "minigame_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiniGameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;           // ID người chơi
    private String type;             // loại hoạt động: DAILY_LOGIN, SPIN_REWARD, REDEEM_VOUCHER
    private String description;      // mô tả hành động

    private Integer pointsEarned;    // ✅ điểm nhận được hoặc bị trừ

    private LocalDateTime createdAt; // ✅ thời điểm ghi nhận

    @PrePersist
    public void prePersist() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }
}
