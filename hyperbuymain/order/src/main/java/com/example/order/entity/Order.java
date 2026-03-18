package com.example.order.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id") // Lưu username (e.g., baodh)
    private String userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // ✅ Tổng tiền trước giảm
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // ✅ Mã voucher (nếu có)
    @Column(length = 100)
    private String voucherCode;

    // ✅ Số tiền giảm
    @Column(precision = 15, scale = 2)
    private BigDecimal discountAmount;

    // ✅ Tổng tiền sau giảm = totalAmount - discountAmount
    @Column(precision = 15, scale = 2)
    private BigDecimal finalAmount;

    @Column(length = 255)
    private String shippingAddressLine1;
    @Column(length = 255)
    private String shippingAddressLine2;
    @Column(length = 100)
    private String shippingCity;
    @Column(length = 20)
    private String shippingPostalCode;
    @Column(length = 100)
    private String shippingCountry;

    @Column(length = 255)
    private String billingAddressLine1;
    @Column(length = 255)
    private String billingAddressLine2;
    @Column(length = 100)
    private String billingCity;
    @Column(length = 20)
    private String billingPostalCode;
    @Column(length = 100)
    private String billingCountry;

    private String paymentMethod;
    private String paymentTransactionId;
    

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        orderDate = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
