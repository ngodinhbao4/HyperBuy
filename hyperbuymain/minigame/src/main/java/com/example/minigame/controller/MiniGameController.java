package com.example.minigame.controller;

import com.example.minigame.dto.RewardInfoDTO;
import com.example.minigame.entity.MiniGameHistory;
import com.example.minigame.service.MiniGameHistoryService;
import com.example.minigame.service.MiniGameService;
import com.example.minigame.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

/**
 * 🎮 MiniGame Controller
 *  - Đăng nhập nhận quà
 *  - Vòng quay may mắn
 *  - Xem lịch sử & tổng kết phần thưởng
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MiniGameController {

    private final MiniGameService miniGameService;
    private final MiniGameHistoryService historyService;
    private final RewardService rewardService;
    private static final Random RANDOM = new Random();

    private enum PrizeType {
        NONE,               // Không trúng
        POINTS_SMALL,       // Điểm thường
        POINTS_MEDIUM,      // Điểm khá
        VOUCHER_SMALL,      // Voucher thường
        VOUCHER_BIG,
        VOUCHER_LEGENDARY    // Voucher hiếm
    }

    /**
     * Hàm random phần thưởng:
     *  - 50%: NONE (không trúng)
     *  - 50% còn lại chia theo độ hiếm:
     *      + 50%: POINTS_SMALL      → 25% tổng thể
     *      + 30%: POINTS_MEDIUM     → 15% tổng thể
     *      + 15%: VOUCHER_SMALL     → 7.5% tổng thể
     *      + 4,95% : VOUCHER_BIG       → 2.475% tổng thể
     *      + 0,05% : VOUCHER_LEGENDARY → 0.025% tổng thể
     */
    private PrizeType randomPrize() {
        double roll = RANDOM.nextDouble(); // [0, 1)

        // Bước 1: 50% không trúng
        if (roll < 0.5) {
            return PrizeType.NONE;
        }

        // Bước 2: chắc chắn trúng -> chia tỉ lệ phần thưởng trong 50% còn lại
        double rewardRoll = RANDOM.nextDouble(); // [0, 1)

        // 0.0  - 0.5  → 50% của nhóm trúng   (POINTS_SMALL)
        // 0.5  - 0.8  → 30% của nhóm trúng   (POINTS_MEDIUM)
        // 0.8  - 0.95 → 15% của nhóm trúng   (VOUCHER_SMALL)
        // 0.95 - 1.0  → 5%  của nhóm trúng   (VOUCHER_BIG)

        if (rewardRoll < 0.50) {
            return PrizeType.POINTS_SMALL;
        } else if (rewardRoll < 0.80) {
            return PrizeType.POINTS_MEDIUM;
        } else if (rewardRoll < 0.95) {
            return PrizeType.VOUCHER_SMALL;
        } else if (rewardRoll < 0.9995) {
            return PrizeType.VOUCHER_BIG;
        } else {
            return PrizeType.VOUCHER_LEGENDARY;
        }
    }


    /**
     * 🎁 Đăng nhập nhận quà (1 lần mỗi ngày)
     */
    @PostMapping("/daily-reward/{userId}")
    public ResponseEntity<String> dailyReward(@PathVariable String userId) {
        if (historyService.hasClaimedToday(userId, "DAILY_REWARD")) {
            return ResponseEntity.badRequest().body("❌ Bạn đã nhận quà hôm nay rồi!");
        }

        // Cộng điểm
        rewardService.addPoints(userId, 10);

        // Lưu lịch sử
        historyService.saveHistory(userId, "DAILY_REWARD", "Nhận 10 điểm khi đăng nhập hôm nay");

        log.info("🎁 User {} nhận quà đăng nhập thành công (+10 điểm)", userId);
        return ResponseEntity.ok("✅ Nhận quà đăng nhập thành công! +10 điểm");
    }

    /**
     * 🎡 Vòng quay may mắn
     */
    @PostMapping("/spin/{userId}")
    public ResponseEntity<String> spin(@PathVariable String userId) {
        if (historyService.hasClaimedToday(userId, "SPIN")) {
            return ResponseEntity.badRequest().body("❌ Hôm nay bạn đã quay rồi!");
        }

        PrizeType prize = randomPrize();
        String message;

        switch (prize) {
            case NONE -> {
                message = "😅 Rất tiếc, bạn chưa trúng thưởng lần này.";
            }
            case POINTS_SMALL -> {
                // Ví dụ: +10 điểm
                rewardService.addPoints(userId, 10);
                message = "🎉 Chúc mừng! Bạn nhận được 10 điểm!";
            }
            case POINTS_MEDIUM -> {
                // Ví dụ: +30 điểm
                rewardService.addPoints(userId, 30);
                message = "🎉 Tuyệt vời! Bạn nhận được 30 điểm!";
            }
            case VOUCHER_SMALL -> {
                // Đổi thành mã voucher có thật, ví dụ "SALE10"
                rewardService.grantVoucherAfterSpin(userId, "SALE10");
                message = "🎊 Chúc mừng! Bạn trúng voucher SALE10!";
            }
            case VOUCHER_BIG -> {
                // Voucher hiếm, ví dụ "SALE50"
                rewardService.grantVoucherAfterSpin(userId, "SALE30");
                message = "🔥 SIÊU HIẾM! Bạn trúng voucher SALE50!";
            }
            case VOUCHER_LEGENDARY -> {
                rewardService.grantVoucherAfterSpin(userId, "SALE50"); 
                message = "💎 HUYỀN THOẠI! Bạn nhận được VOUCHER LEGENDARY SALE50 (tỉ lệ 0.05%)!";
            }
            default -> throw new IllegalStateException("Unexpected value: " + prize);
        }

        // Lưu lịch sử
        historyService.saveHistory(userId, "SPIN", message);
        log.info("🎡 Kết quả quay thưởng của {}: {}", userId, message);

        return ResponseEntity.ok(message);
    }

    /**
     * 📜 Xem lịch sử mini game
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<MiniGameHistory>> getHistory(@PathVariable String userId) {
        log.info("📜 User {} xem lịch sử minigame", userId);
        return ResponseEntity.ok(historyService.getHistoryByUser(userId));
    }

    /**
     * 📊 Xem tổng kết điểm & voucher thật từ voucher-service
     */
    @GetMapping("/summary/{userId}")
    public ResponseEntity<RewardInfoDTO> getRewardSummary(@PathVariable String userId) {
        log.info("📊 User {} xem tổng điểm & voucher hiện có", userId);
        return ResponseEntity.ok(rewardService.getRewardSummary(userId));
    }
}
