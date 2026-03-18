package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(nullable = false, updatable = false)
    private String id; // Sử dụng UUID làm ID cho thanh toán

    @Column(nullable = false, unique = true) // Mỗi order chỉ nên có 1 payment chính (có thể có refund sau)
    private Long orderId; // ID của đơn hàng từ OrderService

    @Column(nullable = false)
    private String userId; // ID của người dùng

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // Số tiền thanh toán

    @Column(nullable = false, length = 3)
    private String currency; // Đơn vị tiền tệ (ví dụ: "VND", "USD")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 50)
    private String paymentMethod; // Ví dụ: "CREDIT_CARD", "COD", "BANK_TRANSFER", "MOMO"

    @Column(length = 255)
    private String paymentGatewayTransactionId; // ID giao dịch từ cổng thanh toán

    @Lob // Cho phép lưu trữ dữ liệu lớn, ví dụ chi tiết lỗi từ cổng thanh toán
    @Column(columnDefinition = "TEXT")
    private String gatewayResponseDetails; // Chi tiết phản hồi từ cổng thanh toán

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "payment_date") // Thời điểm thanh toán thành công hoặc thất bại
    private LocalDateTime paymentDate;


    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        if (this.currency == null) {
            this.currency = "VND"; // Mặc định là VND
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}