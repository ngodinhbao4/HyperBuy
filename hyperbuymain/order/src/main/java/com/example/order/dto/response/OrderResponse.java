package com.example.order.dto.response;

import com.example.order.dto.AddressDTO;
import com.example.order.entity.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String userId;
    private List<OrderItemResponse> items;
    private LocalDateTime orderDate;
    private OrderStatus status;

    // ✅ Tổng trước giảm
    private BigDecimal totalAmount;

    // ✅ Mã voucher + số tiền giảm + tổng sau giảm
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    private AddressDTO shippingAddress;
    private AddressDTO billingAddress;
    private String paymentMethod;
    private String paymentTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
