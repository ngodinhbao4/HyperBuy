package com.example.voucher.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoyaltySpendPointsRequest {
    private String userId;
    private Integer points;
    private String reason;
    private String reference;
}
