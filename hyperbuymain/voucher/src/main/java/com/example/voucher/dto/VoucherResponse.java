package com.example.voucher.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherResponse {
    private Long id;
    private String code;
    private String discountType;  // PERCENT / AMOUNT
    private Double discountValue; // 🔁 đổi từ Integer → Double
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer quantity;
    private String status;        // ACTIVE / INACTIVE
    private Integer pointCost;
}
