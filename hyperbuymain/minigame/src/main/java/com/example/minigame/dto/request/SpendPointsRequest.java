package com.example.minigame.dto.request;

import lombok.Data;

@Data
public class SpendPointsRequest {
    private String userId;
    private Integer points;      // số điểm muốn trừ
    private String reason;       // REDEEM_VOUCHER
    private String reference;    // mã voucher, vd: "SALE10"
}
