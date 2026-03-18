package com.example.minigame.service;

import com.example.minigame.entity.MiniGame;
import com.example.minigame.repository.MiniGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ⚙️ Kiểm tra và thay đổi trạng thái hoạt động của mini game
 */
@Service
@RequiredArgsConstructor
public class MiniGameStatusService {

    private final MiniGameRepository repository;

    /**
     * Kiểm tra xem game có đang ACTIVE hay không
     */
    public boolean isActive(String gameId) {
        return repository.findById(gameId)
                .map(game -> "ACTIVE".equalsIgnoreCase(game.getStatus()))
                .orElse(true); // Nếu không tìm thấy, mặc định bật
    }

    /**
     * Bật hoặc tắt game
     */
    public String toggle(String gameId, boolean active) {
        MiniGame game = repository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy mini game có ID: " + gameId));

        game.setStatus(active ? "ACTIVE" : "INACTIVE");
        repository.save(game);

        return active ? "✅ Đã bật mini game: " + game.getName()
                      : "⛔ Đã tắt mini game: " + game.getName();
    }
}
