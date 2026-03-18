package com.example.minigame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🧾 Thông tin điểm thưởng hiện tại của người dùng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardInfoDTO {
    private String userId;       // ID người chơi
    private int totalPoints;     // Tổng điểm hiện tại
    private int vouchersOwned;   // Số lượng voucher đang có
}
