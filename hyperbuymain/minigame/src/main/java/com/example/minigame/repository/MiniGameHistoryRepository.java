package com.example.minigame.repository;

import com.example.minigame.entity.MiniGameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MiniGameHistoryRepository extends JpaRepository<MiniGameHistory, Long> {

    // ✅ Kiểm tra người dùng đã chơi loại game này trong ngày chưa
    boolean existsByUserIdAndTypeAndCreatedAtBetween(
            String userId,
            String type,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );

    // ✅ Lấy danh sách lịch sử chơi của 1 user (mới nhất trước)
    List<MiniGameHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    // ✅ Lấy toàn bộ lịch sử chơi của user
    List<MiniGameHistory> findAllByUserId(String userId);

    // ✅ Đếm số lượt chơi trong ngày của 1 loại game
    long countByUserIdAndTypeAndCreatedAtBetween(
            String userId,
            String type,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );
}
