package com.example.product.service.impl;

import com.example.product.entity.Product;
import com.example.product.entity.ProductViewHistory;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.ProductViewHistoryRepository;
import com.example.product.service.ProductViewHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductViewHistoryServiceImpl implements ProductViewHistoryService {

    private final ProductRepository productRepository;
    private final ProductViewHistoryRepository historyRepository;

    @Override
    @Transactional
    public void logView(Long productId, String username) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + productId));

            ProductViewHistory history = ProductViewHistory.builder()
                    .product(product)
                    .username(username) // có thể null nếu chưa login
                    .build();

            historyRepository.save(history);
        } catch (Exception e) {
            // Không nên làm fail luôn API chỉ vì lỗi log history
            log.error("Lỗi khi ghi lịch sử xem sản phẩm {} cho user {}: {}", productId, username, e.getMessage());
        }
    }
}
