package com.example.minigame.service;
import com.example.minigame.dto.reponse.SpendPointsResponse;
import com.example.minigame.dto.request.SpendPointsRequest;
import com.example.minigame.entity.LoyaltyAccount;
import com.example.minigame.entity.LoyaltyTransaction;

import java.util.List;

/**
 * 🎯 Interface định nghĩa các chức năng chính của hệ thống tích điểm
 */
public interface LoyaltyService {

    // ✅ Lấy tài khoản điểm của user (nếu chưa có thì tạo mới)
    LoyaltyAccount getOrCreateAccount(String userId);

    // ✅ Cộng điểm (ví dụ: đăng nhập mỗi ngày, chơi minigame)
    LoyaltyTransaction earnPoints(String userId, int points, String description);


    // ✅ Lấy lịch sử giao dịch của người dùng
    List<LoyaltyTransaction> getTransactionHistory(String userId);

    SpendPointsResponse spendPoints(SpendPointsRequest request);
}
