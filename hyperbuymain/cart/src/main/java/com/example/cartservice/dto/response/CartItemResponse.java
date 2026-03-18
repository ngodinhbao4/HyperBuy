package com.example.cartservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemResponse {
    private Long id;
    private String productId;
    private String productName;
    private String imageUrl;    // Từ ProductService
    private Integer quantity;
    private BigDecimal priceAtAddition;
    private BigDecimal currentPrice; // Từ ProductService (optional)
    private BigDecimal lineItemTotal;
    private LocalDateTime addedAt;
}
