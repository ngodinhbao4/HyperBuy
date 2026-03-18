package com.example.cartservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartResponse {
    private String id;
    private String userId;
    private List<CartItemResponse> items;
    private Integer totalUniqueItems;
    private Integer totalQuantity;
    private BigDecimal grandTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
