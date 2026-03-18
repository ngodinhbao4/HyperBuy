package com.example.minigame.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 📜 Lịch sử giao dịch điểm thưởng (cộng hoặc trừ)
 */
@Entity
@Table(name = "LoyaltyTransaction")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String transactionId;

    private String userId;             // Người được cộng/trừ điểm
    private String type;               // EARN / SPEND
    private Integer amount;            // Số điểm thay đổi (+ hoặc -)
    private String description;        // Mô tả (VD: “Đăng nhập nhận điểm”, “Đổi voucher SALE50”)
    private LocalDateTime createdAt;   // Thời gian thực hiện
}
