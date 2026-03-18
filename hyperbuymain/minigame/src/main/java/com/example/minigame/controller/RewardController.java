package com.example.minigame.controller;

import com.example.minigame.dto.RewardInfoDTO;
import com.example.minigame.dto.UserVoucherDTO;
import com.example.minigame.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    // 🪙 Cộng điểm cho user (dùng để test nhanh)
    @PostMapping("/{userId}/add-points")
    public ResponseEntity<Void> addPoints(
            @PathVariable String userId,
            @RequestParam int points
    ) {
        rewardService.addPoints(userId, points);
        return ResponseEntity.ok().build();
    }

    // 💱 Đổi điểm lấy voucher (logic chính)
    @PostMapping("/{userId}/redeem")
    public ResponseEntity<Void> redeemVoucher(
            @PathVariable String userId,
            @RequestParam String code
    ) {
        rewardService.redeemVoucher(userId, code);
        return ResponseEntity.ok().build();
    }

    // 📊 Xem tổng quan điểm + số lượng voucher
    @GetMapping("/{userId}/summary")
    public ResponseEntity<RewardInfoDTO> getSummary(
            @PathVariable String userId
    ) {
        RewardInfoDTO summary = rewardService.getRewardSummary(userId);
        return ResponseEntity.ok(summary);
    }

    // 🎟️ Xem danh sách voucher của user
    @GetMapping("/{userId}/vouchers")
    public ResponseEntity<List<UserVoucherDTO>> getUserVouchers(
            @PathVariable String userId
    ) {
        List<UserVoucherDTO> vouchers = rewardService.getUserVouchers(userId);
        return ResponseEntity.ok(vouchers);
    }
}
