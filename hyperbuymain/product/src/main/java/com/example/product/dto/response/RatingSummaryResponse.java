package com.example.product.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingSummaryResponse {
    private Long productId;
    private double averageRating;
    private long totalRatings;
    private long count1Star;
    private long count2Star;
    private long count3Star;
    private long count4Star;
    private long count5Star;
}
