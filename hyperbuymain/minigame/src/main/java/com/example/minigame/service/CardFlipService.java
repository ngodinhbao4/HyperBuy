package com.example.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 🃏 Mini game Lật thẻ nâng cao (6 ô, được chọn 2 lần/ngày)
 */
@Service
@RequiredArgsConstructor
public class CardFlipService {

    private final RewardService rewardService;
    private final MiniGameHistoryService historyService;
    private final MiniGameStatusService miniGameStatusService;

    private static final String GAME_ID = "CARD_FLIP_ADVANCED";

    public String play(String userId, int choice) {
        if (choice < 1 || choice > 6) {
            return "❌ Lựa chọn không hợp lệ! Vui lòng chọn từ 1 đến 6.";
        }

        // Kiểm tra trạng thái bật/tắt game
        if (!miniGameStatusService.isActive(GAME_ID)) {
            return "🚫 Mini game Lật thẻ nâng cao đang tạm tắt. Vui lòng quay lại sau!";
        }

        // Đếm số lượt chơi trong ngày
        long playsToday = historyService.countTodayPlays(userId, GAME_ID);
        if (playsToday >= 2) {
            return "⚠️ Bạn đã sử dụng hết 2 lượt chơi hôm nay!";
        }

        // 6 thẻ, chỉ 3 thẻ có thưởng
        List<Integer> winningCards = getWinningCards(3, 6);
        String message;

        if (winningCards.contains(choice)) {
            int rewardType = new Random().nextInt(3); // 0: 20đ, 1: 50đ, 2: voucher
            switch (rewardType) {
                case 0 -> {
                    rewardService.addPoints(userId, 20);
                    message = "🎉 Bạn trúng thẻ +20 điểm!";
                }
                case 1 -> {
                    rewardService.addPoints(userId, 50);
                    message = "🏆 Xuất sắc! Bạn nhận được 50 điểm!";
                }
                default -> {
                    rewardService.grantVoucherAfterSpin(userId, "SALE20");
                    message = "🎁 Bạn trúng voucher giảm giá 20%!";
                }
            }
        } else {
            message = "😅 Thẻ bạn chọn không trúng.";
        }

        // Lưu lịch sử
        historyService.saveHistory(userId, GAME_ID, message);

        return message + " (Lượt chơi thứ " + (playsToday + 1) + " trong ngày)";
    }

    /**
     * Sinh ngẫu nhiên danh sách thẻ trúng
     */
    private List<Integer> getWinningCards(int numberOfWinningCards, int totalCards) {
        List<Integer> cards = new ArrayList<>();
        for (int i = 1; i <= totalCards; i++) {
            cards.add(i);
        }
        Collections.shuffle(cards);
        return cards.subList(0, numberOfWinningCards);
    }
}
