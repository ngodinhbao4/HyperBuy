package com.example.minigame.service.impl;

import com.example.minigame.client.VoucherClient;
import com.example.minigame.service.MiniGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 🧩 Kết nối MiniGame với Voucher Service qua FeignClient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiniGameServiceImpl implements MiniGameService {

    private final VoucherClient voucherClient;

    /**
     * 🎁 Gọi sang Voucher Service để phát voucher cho user
     */
    @Override
    public void issueVoucherToUser(String userId, String code) {
        try {
            log.info("🎯 Gửi yêu cầu phát voucher '{}' cho user {}", code, userId);
            voucherClient.issueVoucher(userId, code);
            log.info("✅ Phát voucher thành công cho user {}", userId);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi voucher-service: {}", e.getMessage());
            throw new RuntimeException("Không thể phát voucher cho user: " + userId);
        }
    }
}
