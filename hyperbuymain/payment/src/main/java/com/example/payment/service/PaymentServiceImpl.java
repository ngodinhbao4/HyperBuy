// File: payment-service/src/main/java/com/example/payment/service/PaymentServiceImpl.java
package com.example.payment.service;

import com.example.payment.dto.request.PaymentRequest;
import com.example.payment.dto.response.PaymentResponse;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.exception.ErrorCodePayment;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.MockPaymentGateway;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException; // Import DataAccessException
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway mockPaymentGateway;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Đang xử lý thanh toán cho orderId: {}, userId: {}, số tiền: {}",
                paymentRequest.getOrderId(), paymentRequest.getUserId(), paymentRequest.getAmount());

        paymentRepository.findByOrderId(paymentRequest.getOrderId()).ifPresent(existingPayment -> {
            if (existingPayment.getStatus() == PaymentStatus.SUCCESS || existingPayment.getStatus() == PaymentStatus.PENDING) {
                logger.warn("Thanh toán đã được xử lý hoặc đang chờ cho orderId: {}. Trạng thái hiện tại: {}",
                        paymentRequest.getOrderId(), existingPayment.getStatus());
                throw new PaymentException(ErrorCodePayment.PAYMENT_ALREADY_PROCESSED,
                        "Thanh toán cho đơn hàng " + paymentRequest.getOrderId() + " đã được xử lý hoặc đang chờ.");
            }
        });

        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setUserId(paymentRequest.getUserId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setCurrency(paymentRequest.getCurrency().toUpperCase());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataAccessException e) {
            logger.error("Lỗi database khi lưu thanh toán ban đầu cho orderId: {}: {}", paymentRequest.getOrderId(), e.getMessage(), e);
            throw new PaymentException(ErrorCodePayment.PAYMENT_PROCESSING_FAILED, "Không thể khởi tạo giao dịch thanh toán do lỗi cơ sở dữ liệu.", e);
        }
        logger.info("Bản ghi thanh toán đã được tạo với ID: {} và trạng thái PENDING cho orderId: {}", savedPayment.getId(), savedPayment.getOrderId());

        MockPaymentGateway.GatewayPaymentResult gatewayResult;
        try {
            gatewayResult = mockPaymentGateway.processPayment(paymentRequest);
        } catch (Exception e) { // Bắt lỗi chung từ mock gateway
            logger.error("Lỗi khi gọi cổng thanh toán mô phỏng cho orderId: {}: {}", paymentRequest.getOrderId(), e.getMessage(), e);
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setPaymentDate(LocalDateTime.now());
            savedPayment.setGatewayResponseDetails("Lỗi khi xử lý với cổng thanh toán: " + e.getMessage());
            try {
                paymentRepository.save(savedPayment); // Cố gắng lưu trạng thái lỗi
            } catch (DataAccessException dae) {
                logger.error("Lỗi database khi cập nhật trạng thái thanh toán FAILED cho orderId: {}: {}", paymentRequest.getOrderId(), dae.getMessage(), dae);
                // Ném lỗi gốc từ gateway nếu không lưu được trạng thái lỗi
                throw new PaymentException(ErrorCodePayment.PAYMENT_GATEWAY_ERROR, "Lỗi khi giao tiếp với cổng thanh toán và cập nhật trạng thái thất bại.", e);
            }
            throw new PaymentException(ErrorCodePayment.PAYMENT_GATEWAY_ERROR, "Lỗi khi giao tiếp với cổng thanh toán.", e);
        }

        if (gatewayResult.isSuccessful()) {
            savedPayment.setStatus(PaymentStatus.SUCCESS);
            savedPayment.setPaymentGatewayTransactionId(gatewayResult.getTransactionId());
            savedPayment.setPaymentDate(LocalDateTime.now());
            logger.info("Thanh toán THÀNH CÔNG cho orderId: {}. Gateway transactionId: {}",
                    savedPayment.getOrderId(), gatewayResult.getTransactionId());
        } else {
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setPaymentDate(LocalDateTime.now());
            logger.warn("Thanh toán THẤT BẠI cho orderId: {}. Lý do: {}",
                    savedPayment.getOrderId(), gatewayResult.getMessage());
        }
        savedPayment.setGatewayResponseDetails(gatewayResult.getMessage());

        Payment finalPayment;
        try {
            finalPayment = paymentRepository.save(savedPayment);
        } catch (DataAccessException e) {
            logger.error("Lỗi database khi lưu trạng thái thanh toán cuối cùng cho orderId: {}: {}", paymentRequest.getOrderId(), e.getMessage(), e);
            // Nếu không lưu được trạng thái cuối cùng, đây là một vấn đề.
            // Trả về lỗi xử lý thanh toán, vì trạng thái cuối cùng không được lưu.
            throw new PaymentException(ErrorCodePayment.PAYMENT_PROCESSING_FAILED, "Không thể cập nhật trạng thái thanh toán cuối cùng do lỗi cơ sở dữ liệu.", e);
        }

        PaymentResponse response = mapEntityToResponse(finalPayment);
        response.setRedirectUrl(gatewayResult.getRedirectUrl());
        response.setMessage(gatewayResult.getMessage());
        return response;
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        logger.debug("Đang lấy thông tin thanh toán cho orderId: {}", orderId);
        try {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new PaymentException(ErrorCodePayment.PAYMENT_NOT_FOUND_FOR_ORDER,
                            "Không tìm thấy thanh toán cho đơn hàng ID: " + orderId));
            return mapEntityToResponse(payment);
        } catch (DataAccessException e) {
            logger.error("Lỗi database khi lấy thanh toán cho orderId: {}: {}", orderId, e.getMessage(), e);
            throw new PaymentException(ErrorCodePayment.PAYMENT_PROCESSING_FAILED, "Lỗi truy vấn cơ sở dữ liệu khi lấy thông tin thanh toán.", e);
        }
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        logger.debug("Đang lấy thông tin thanh toán theo paymentId: {}", paymentId);
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentException(ErrorCodePayment.PAYMENT_NOT_FOUND,
                            "Không tìm thấy thanh toán với ID: " + paymentId));
            return mapEntityToResponse(payment);
        } catch (DataAccessException e) {
            logger.error("Lỗi database khi lấy thanh toán theo paymentId: {}: {}", paymentId, e.getMessage(), e);
            throw new PaymentException(ErrorCodePayment.PAYMENT_PROCESSING_FAILED, "Lỗi truy vấn cơ sở dữ liệu khi lấy thông tin thanh toán.", e);
        }
    }

    private PaymentResponse mapEntityToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setUserId(payment.getUserId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentGatewayTransactionId(payment.getPaymentGatewayTransactionId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setPaymentDate(payment.getPaymentDate());
        return response;
    }
}
