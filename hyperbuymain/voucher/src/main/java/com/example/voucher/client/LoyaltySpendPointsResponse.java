package com.example.voucher.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoyaltySpendPointsResponse {
    private boolean success;
    private String message;
    private Integer remainingPoints;
}
