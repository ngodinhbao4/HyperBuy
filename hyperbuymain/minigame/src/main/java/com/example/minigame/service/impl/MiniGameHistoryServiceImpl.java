package com.example.minigame.service.impl;

import com.example.minigame.entity.MiniGameHistory;
import com.example.minigame.repository.MiniGameHistoryRepository;
import com.example.minigame.service.MiniGameHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * ⚙️ Xử lý nghiệp vụ cho MiniGameHistory (đăng nhập nhận quà, vòng quay may mắn)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiniGameHistoryServiceImpl implements MiniGameHistoryService {

    private final MiniGameHistoryRepository historyRepository;

    /**
     * 🧩 Kiểm tra xem user đã nhận thưởng hôm nay chưa
     */
    @Override
    public boolean hasClaimedToday(String userId, String type) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        boolean claimed = historyRepository.existsByUserIdAndTypeAndCreatedAtBetween(
                userId, type, startOfDay, endOfDay);
        log.debug("👀 hasClaimedToday({}, {}): {}", userId, type, claimed);
        return claimed;
    }

    /**
     * 📝 Lưu lịch sử mini game
     */
    @Override
    public void saveHistory(String userId, String type, String description) {
        MiniGameHistory history = new MiniGameHistory();
        history.setUserId(userId);
        history.setType(type);
        history.setDescription(description);
        history.setCreatedAt(LocalDateTime.now());

        historyRepository.save(history);
        log.info("💾 Đã lưu lịch sử mini game: user={}, type={}, desc={}", userId, type, description);
    }

    /**
     * 📄 Lấy toàn bộ lịch sử của user
     */
    @Override
    public List<MiniGameHistory> getHistoryByUser(String userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 🎯 Đếm số lượt chơi hôm nay của người dùng theo loại game
     */
    @Override
    public long countTodayPlays(String userId, String type) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return historyRepository.countByUserIdAndTypeAndCreatedAtBetween(
                userId, type, startOfDay, endOfDay);
    }
}
