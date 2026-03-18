package com.example.product.dto.response;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null trong ProductResponse
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private String imageUrl;
    private boolean isActive;
    private Long categoryId; // Thêm categoryId
    private String categoryName; // Thêm categoryName cho tiện hiển thị
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SellerInfo sellerInfo; // Added field

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SellerInfo {
        private String storeId;
        private String storeName;
        private String userId;
        private String username;
    }
}
