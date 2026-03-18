package com.example.minigame.service;

import java.util.List;

import com.example.minigame.dto.RewardInfoDTO;
import com.example.minigame.dto.UserVoucherDTO;

public interface RewardService {

    /**
     * 🪙 Tặng điểm cho người chơi khi đăng nhập
     * @param userId ID người chơi
     * @param points số điểm được tặng
     */
    void addPoints(String userId, int points);

    /**
     * 💱 Đổi điểm sang voucher (nếu đủ điểm)
     * @param userId ID người chơi
     * @param code Mã voucher muốn đổi
     */
    void redeemVoucher(String userId, String code);

    /**
     * 🎡 Khi người chơi quay trúng thưởng, phát voucher ngay
     * @param userId ID người chơi
     * @param voucherCode Mã voucher trúng được
     */
    void grantVoucherAfterSpin(String userId, String voucherCode);

    RewardInfoDTO getRewardSummary(String userId);

    // 🔹 Lấy danh sách voucher mà user đang có
    List<UserVoucherDTO> getUserVouchers(String userId);
}
