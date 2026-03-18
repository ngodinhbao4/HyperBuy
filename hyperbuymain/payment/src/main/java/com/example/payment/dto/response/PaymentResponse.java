package com.example.payment.dto.response;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.payment.entity.PaymentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private Long orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
    private String paymentGatewayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDate; // Thời điểm thanh toán thành công/thất bại
    private String redirectUrl; // URL để chuyển hướng người dùng đến cổng thanh toán (nếu có)
    private String message; // Thông điệp bổ sung (ví dụ: "Thanh toán thành công")
}