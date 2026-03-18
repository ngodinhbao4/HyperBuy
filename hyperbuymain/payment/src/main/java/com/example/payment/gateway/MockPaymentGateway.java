package com.example.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.payment.dto.request.PaymentRequest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Random;

@Component // Để có thể inject vào PaymentServiceImpl
public class MockPaymentGateway {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentGateway.class);
    private final Random random = new Random();

    public static class GatewayPaymentResult {
        private boolean successful;
        private String transactionId;
        private String message;
        private String redirectUrl; // Nếu cổng thanh toán yêu cầu chuyển hướng

        public GatewayPaymentResult(boolean successful, String transactionId, String message, String redirectUrl) {
            this.successful = successful;
            this.transactionId = transactionId;
            this.message = message;
            this.redirectUrl = redirectUrl;
        }

        public boolean isSuccessful() { return successful; }
        public String getTransactionId() { return transactionId; }
        public String getMessage() { return message; }
        public String getRedirectUrl() { return redirectUrl; }
    }

    public GatewayPaymentResult processPayment(PaymentRequest paymentRequest) {
        logger.info("MockPaymentGateway: Processing payment for orderId: {}, amount: {} {}",
                paymentRequest.getOrderId(), paymentRequest.getAmount(), paymentRequest.getCurrency());

        // Mô phỏng logic xử lý thanh toán
        // Ví dụ: dựa trên số tiền hoặc một yếu tố ngẫu nhiên để thành công/thất bại
        boolean paymentSuccess;
        String message;
        String transactionId = null;
        String redirectUrl = null;

        // Đối với COD, luôn thành công (vì thanh toán khi nhận hàng)
        if ("COD".equalsIgnoreCase(paymentRequest.getPaymentMethod())) {
            paymentSuccess = true;
            message = "Thanh toán COD được chấp nhận, sẽ được xử lý khi giao hàng.";
            transactionId = "COD-" + UUID.randomUUID().toString().substring(0, 8);
            logger.info("MockPaymentGateway: COD payment for orderId: {} marked as successful.", paymentRequest.getOrderId());
        } else if (paymentRequest.getAmount().compareTo(new BigDecimal("1000000")) > 0 && random.nextInt(10) < 2) {
            // 20% thất bại nếu số tiền lớn hơn 1,000,000
            paymentSuccess = false;
            message = "Thanh toán thất bại: Giao dịch bị từ chối bởi cổng thanh toán (số tiền lớn).";
            logger.warn("MockPaymentGateway: Payment FAILED for orderId: {} (large amount simulation).", paymentRequest.getOrderId());
        } else if (random.nextInt(10) < 1) {
            // 10% thất bại ngẫu nhiên
            paymentSuccess = false;
            message = "Thanh toán thất bại: Lỗi ngẫu nhiên từ cổng thanh toán.";
            logger.warn("MockPaymentGateway: Payment FAILED for orderId: {} (random simulation).", paymentRequest.getOrderId());
        } else {
            paymentSuccess = true;
            message = "Thanh toán thành công qua cổng thanh toán mô phỏng.";
            transactionId = "MOCK_GW-" + UUID.randomUUID().toString();
            logger.info("MockPaymentGateway: Payment SUCCESSFUL for orderId: {}. TransactionId: {}", paymentRequest.getOrderId(), transactionId);

            // Một số cổng thanh toán có thể yêu cầu chuyển hướng (ví dụ: 3D Secure, PayPal)
            // if ("CREDIT_CARD_3DS".equals(paymentRequest.getPaymentMethod())) {
            //     redirectUrl = "http://mock-gateway.com/3ds_redirect?transactionId=" + transactionId;
            // }
        }
        return new GatewayPaymentResult(paymentSuccess, transactionId, message, redirectUrl);
    }

    // (Tùy chọn) Mô phỏng xử lý hoàn tiền
    // public boolean processRefund(String originalTransactionId, BigDecimal amount) { ... }
}