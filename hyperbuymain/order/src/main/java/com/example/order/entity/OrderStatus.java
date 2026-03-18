package com.example.order.entity;

public enum OrderStatus {
    PENDING,        // Chờ xử lý / Chờ thanh toán
    PROCESSING,     // Đang xử lý (ví dụ: đã thanh toán, chờ đóng gói)
    CONFIRMED,      // Đã xác nhận (sau khi thanh toán thành công)
    SHIPPED,        // Đã giao cho đơn vị vận chuyển
    DELIVERED,      // Đã giao thành công
    CANCELLED,      // Đã hủy
    FAILED,          // Thất bại (ví dụ: thanh toán thất bại)
    PAID
}