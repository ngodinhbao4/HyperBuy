package com.example.minigame.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.minigame.entity.LoyaltyTransaction;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, String> {

    // 🔍 Lấy lịch sử giao dịch theo userId
    List<LoyaltyTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}
