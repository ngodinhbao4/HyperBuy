package com.example.product.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RatingResponse {
    private Long id;
    private Long productId;
    private String username;
    private int ratingValue;
    private String comment;
    private LocalDateTime createdAt;
}
