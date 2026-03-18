package com.example.product.repository;

import com.example.product.entity.ProductViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductViewHistoryRepository extends JpaRepository<ProductViewHistory, Long> {

    // Lịch sử xem theo user
    List<ProductViewHistory> findByUsernameOrderByViewedAtDesc(String username);

    // Lịch sử xem theo product
    List<ProductViewHistory> findByProductIdOrderByViewedAtDesc(Long productId);
}
