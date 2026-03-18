package com.example.order.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal priceAtAddition;
    private BigDecimal currentPrice;
    private BigDecimal lineItemTotal;
    private LocalDateTime addedAt;
}