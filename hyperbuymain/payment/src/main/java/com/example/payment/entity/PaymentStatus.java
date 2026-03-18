package com.example.payment.entity;

public enum PaymentStatus {
    PENDING,    // Thanh toán đang chờ xử lý
    SUCCESS,    // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    REFUND_PENDING, // Yêu cầu hoàn tiền đang chờ
    REFUNDED,   // Đã hoàn tiền
    CANCELLED   // Thanh toán đã bị hủy (ví dụ: do order bị hủy trước khi thanh toán)
}