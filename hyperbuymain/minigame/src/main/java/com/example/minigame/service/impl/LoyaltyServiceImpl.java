package com.example.minigame.service.impl;

import com.example.minigame.dto.reponse.SpendPointsResponse;
import com.example.minigame.dto.request.SpendPointsRequest;
import com.example.minigame.entity.LoyaltyAccount;
import com.example.minigame.entity.LoyaltyTransaction;
import com.example.minigame.entity.MiniGameHistory;
import com.example.minigame.repository.LoyaltyAccountRepository;
import com.example.minigame.repository.LoyaltyTransactionRepository;
import com.example.minigame.repository.MiniGameHistoryRepository;
import com.example.minigame.service.LoyaltyService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final MiniGameHistoryRepository miniGameHistoryRepository;

    @Override
    @Transactional
    public SpendPointsResponse spendPoints(SpendPointsRequest request) {
        String userId = request.getUserId();
        Integer pointsToSpend = request.getPoints();

        if (pointsToSpend == null || pointsToSpend <= 0) {
            return new SpendPointsResponse(false, "Số điểm yêu cầu không hợp lệ", null);
        }

        LoyaltyAccount account = loyaltyAccountRepository.findById(userId)
                .orElse(null);

        if (account == null) {
            return new SpendPointsResponse(false, "Người dùng chưa có tài khoản điểm", null);
        }

        if (account.getPoints() == null) {
            account.setPoints(0);
        }

        if (account.getPoints() < pointsToSpend) {
            return new SpendPointsResponse(false, "Không đủ điểm để thực hiện giao dịch", account.getPoints());
        }

        // 1️⃣ Trừ điểm
        account.setPoints(account.getPoints() - pointsToSpend);
        account.setUpdatedAt(LocalDateTime.now());
        loyaltyAccountRepository.save(account);

        // 2️⃣ Ghi LoyaltyTransaction
        LoyaltyTransaction tx = LoyaltyTransaction.builder()
                .userId(userId)
                .type("SPEND")
                .amount(-pointsToSpend)
                .description(request.getReason() + " - " + request.getReference()) // VD: REDEEM_VOUCHER - SALE10
                .createdAt(LocalDateTime.now())
                .build();
        loyaltyTransactionRepository.save(tx);

        // 3️⃣ Ghi MiniGameHistory (tùy biến cho thống kê)
        MiniGameHistory history = MiniGameHistory.builder()
                .userId(userId)
                .type("REDEEM_VOUCHER")
                .description("Đổi voucher " + request.getReference() + " bằng " + pointsToSpend + " điểm")
                .pointsEarned(-pointsToSpend)
                .build();
        miniGameHistoryRepository.save(history);

        return new SpendPointsResponse(true, "Trừ điểm thành công", account.getPoints());
    }

    @Override
    public LoyaltyAccount getOrCreateAccount(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOrCreateAccount'");
    }

    @Override
    public LoyaltyTransaction earnPoints(String userId, int points, String description) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'earnPoints'");
    }

    @Override
    public List<LoyaltyTransaction> getTransactionHistory(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTransactionHistory'");
    }
}
