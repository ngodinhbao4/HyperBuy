package com.hyperbuy.recommendation.util;

import com.hyperbuy.recommendation.model.Recommendation;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationGenerator {

    public List<Recommendation> generateForUser(Long userId) {
        // Giả lập: gọi Product Service + UserActivity để lấy dữ liệu
        // (sau sẽ dùng FeignClient để gọi thật)
        List<Long> purchasedProductIds = List.of(1L, 2L, 5L); // ví dụ user đã mua
        List<Long> similarProductIds = List.of(3L, 4L, 6L);   // sản phẩm tương tự

        List<Recommendation> result = new ArrayList<>();
        int score = 100;
        for (Long id : similarProductIds) {
            result.add(Recommendation.builder()
                    .userId(userId)
                    .productId(id)
                    .score((double) score)
                    .build());
            score -= 10;
        }
        return result;
    }
}
