package com.example.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.minigame.entity.MiniGame;
import com.example.minigame.entity.MiniGame.GameStatus;

import java.util.List;

@Repository
public interface MiniGameRepository extends JpaRepository<MiniGame, String> {
    List<MiniGame> findByStatus(GameStatus status);
}
