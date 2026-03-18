package com.example.minigame.controller;

import com.example.minigame.service.CardFlipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 🎴 Mini game Lật thẻ nâng cao (6 ô, 2 lượt mỗi ngày)
 */
@RestController
@RequestMapping("/card-flip-advanced")
@RequiredArgsConstructor
public class CardFlipController {

    private final CardFlipService cardFlipAdvancedService;

    @PostMapping("/{userId}")
    public ResponseEntity<String> play(@PathVariable String userId, @RequestParam int choice) {
        return ResponseEntity.ok(cardFlipAdvancedService.play(userId, choice));
    }
}
