package com.example.minigame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 🕹️ Lịch sử chi tiết hoạt động mini game
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiniGameHistoryDTO {
    private String type;         // DAILY_REWARD, SPIN, REDEEM,...
    private String description;  // Nội dung (VD: "Trúng voucher SALE50")
    private LocalDateTime time;  // Thời điểm diễn ra
    private int pointsChange;    // Số điểm nhận được (có thể = 0)
    private String rewardName;   // Tên phần thưởng (VD: "Voucher SALE50")
}
