package com.example.order.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private int stockQuantity;
    private boolean active; // Tương ứng với isActive từ entity Product
    private boolean deleted; // Tương ứng với isDeleted từ entity Product
    private String imageUrl;
    private SellerInfo sellerInfo;
    @Data
    public static class SellerInfo {
        private String storeId;
        private String userId;
        private String username;
    }
}