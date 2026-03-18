package com.example.minigame.service.impl;

import com.example.minigame.client.VoucherClient;
import com.example.minigame.dto.RewardInfoDTO;
import com.example.minigame.dto.UserVoucherDTO;
import com.example.minigame.entity.LoyaltyAccount;
import com.example.minigame.repository.LoyaltyAccountRepository;
import com.example.minigame.service.RewardService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardServiceImpl implements RewardService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final VoucherClient voucherClient;

    /**
     * 🪙 Cộng điểm cho user (nếu chưa có tài khoản thì tạo mới)
     */
    @Override
    @Transactional
    public void addPoints(String userId, int points) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    LoyaltyAccount newAcc = new LoyaltyAccount();
                    newAcc.setUserId(userId);
                    newAcc.setPoints(0);
                    return loyaltyAccountRepository.save(newAcc);
                });

        account.setPoints(account.getPoints() + points);
        loyaltyAccountRepository.save(account);

        log.info("✅ Đã cộng {} điểm cho user {}", points, userId);
    }

    /**
     * 💱 Đổi điểm sang voucher (FLOW MỚI):
     *  - Không tự trừ điểm ở đây nữa
     *  - Gọi sang voucher-service để:
     *      + kiểm tra pointCost của voucher
     *      + gọi loyalty-service trừ điểm
     *      + phát voucher cho user
     */
    @Override
    @Transactional
    public void redeemVoucher(String userId, String code) {
        try {
            log.info("🎯 Yêu cầu đổi điểm lấy voucher '{}' cho user {}", code, userId);

            // Gọi voucher-service API mới
            UserVoucherDTO userVoucher = voucherClient.redeemVoucherByPoints(userId, code);

            log.info("🎁 Người dùng {} đã đổi điểm thành công để nhận voucher {}", userId, code);
        } catch (Exception e) {
            log.error("❌ Lỗi khi đổi điểm lấy voucher cho user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Không thể đổi điểm lấy voucher: " + e.getMessage());
        }
    }

    /**
     * 🎡 Khi quay trúng thưởng
     */
    @Override
    @Transactional
    public void grantVoucherAfterSpin(String userId, String voucherCode) {
        try {
            voucherClient.issueVoucher(userId, voucherCode);
            log.info("🎊 Người chơi {} nhận được voucher {} sau khi quay thưởng", userId, voucherCode);
        } catch (Exception e) {
            log.error("❌ Lỗi khi phát voucher cho user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 📊 Lấy tổng kết điểm thưởng & số voucher (từ voucher-service)
     */
    @Override
    public RewardInfoDTO getRewardSummary(String userId) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId).orElse(null);
        int totalPoints = account != null ? account.getPoints() : 0;

        // Gọi sang voucher-service để đếm voucher thật
        List<UserVoucherDTO> vouchers = voucherClient.getUserVouchers(userId);
        int voucherCount = vouchers != null ? vouchers.size() : 0;

        return new RewardInfoDTO(userId, totalPoints, voucherCount);
    }

    /**
     * 🎟️ Lấy danh sách voucher của user (từ voucher-service)
     */
    @Override
    public List<UserVoucherDTO> getUserVouchers(String userId) {
        return voucherClient.getUserVouchers(userId);
    }
}
