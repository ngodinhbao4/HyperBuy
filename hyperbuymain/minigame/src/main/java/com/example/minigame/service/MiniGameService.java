package com.example.minigame.service;

/**
 * 🎯 Interface định nghĩa các hành động chính của MiniGame
 * - Gọi sang voucher-service khi người chơi trúng thưởng
 */
public interface MiniGameService {

    /**
     * 🎁 Phát voucher cho người chơi khi trúng thưởng
     * @param userId ID của người chơi
     * @param code Mã voucher (ví dụ SALE50)
     */
    void issueVoucherToUser(String userId, String code);
}
