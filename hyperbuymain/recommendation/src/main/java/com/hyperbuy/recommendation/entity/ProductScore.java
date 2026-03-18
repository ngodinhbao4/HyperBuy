package com.hyperbuy.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔹 Lớp này dùng để tạm lưu thông tin điểm gợi ý cho sản phẩm.
 * Nó không ánh xạ với bảng trong database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductScore {
    private Long productId;   // Mã sản phẩm
    private Double score;     // Điểm gợi ý (Recommendation Score)
}
