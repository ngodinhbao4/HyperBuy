package com.example.minigame.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.minigame.entity.LoyaltyAccount;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, String> {
    Optional<LoyaltyAccount> findByUserId(String userId);
}
