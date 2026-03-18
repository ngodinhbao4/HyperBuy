package com.example.minigame.service;

import com.example.minigame.entity.MiniGameHistory;

import java.util.List;

/**
 * 📜 Dịch vụ quản lý lịch sử chơi mini game (đăng nhập & vòng quay)
 */
public interface MiniGameHistoryService {

    /**
     * 🧩 Kiểm tra xem user đã tham gia mini game cụ thể trong ngày chưa
     * @param userId ID người dùng
     * @param type Loại mini game (ví dụ: "DAILY_REWARD", "LUCKY_SPIN")
     * @return true nếu đã chơi hôm nay
     */
    boolean hasClaimedToday(String userId, String type);

    /**
     * 📝 Lưu lịch sử mini game
     * @param userId ID người dùng
     * @param type Loại mini game
     * @param description Mô tả phần thưởng hoặc kết quả
     */
    void saveHistory(String userId, String type, String description);

    /**
     * 📄 Lấy danh sách lịch sử mini game của 1 user
     */
    List<MiniGameHistory> getHistoryByUser(String userId);

    long countTodayPlays(String userId, String action);
}
