package com.example.payment.service;

import org.springframework.stereotype.Service;

import com.example.payment.dto.request.PaymentRequest;
import com.example.payment.dto.response.PaymentResponse;

@Service
public interface PaymentService {
    /**
     * Xử lý một yêu cầu thanh toán mới.
     *
     * @param paymentRequest Thông tin chi tiết của yêu cầu thanh toán.
     * @return PaymentResponse chứa kết quả của giao dịch thanh toán.
     */
    PaymentResponse processPayment(PaymentRequest paymentRequest);

    /**
     * Lấy thông tin chi tiết của một thanh toán dựa trên ID đơn hàng.
     *
     * @param orderId ID của đơn hàng.
     * @return PaymentResponse chứa thông tin thanh toán, hoặc ném exception nếu không tìm thấy.
     */
    PaymentResponse getPaymentByOrderId(Long orderId);

    /**
     * Lấy thông tin chi tiết của một thanh toán dựa trên ID thanh toán.
     *
     * @param paymentId ID của thanh toán.
     * @return PaymentResponse chứa thông tin thanh toán, hoặc ném exception nếu không tìm thấy.
     */
    PaymentResponse getPaymentById(String paymentId);

    // (Tùy chọn)
    // PaymentResponse processRefund(String paymentId, BigDecimal amountToRefund);
    // PaymentResponse updatePaymentStatusFromWebhook(String gatewayTransactionId, String newStatus, String details);
}