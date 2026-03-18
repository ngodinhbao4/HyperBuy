// File: payment-service/src/main/java/com/example/paymentservice/controller/PaymentController.java
package com.example.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Nếu cần lấy thông tin user từ token
import org.springframework.web.bind.annotation.*;

import com.example.payment.dto.request.PaymentRequest;
import com.example.payment.dto.response.ApiResponse;
import com.example.payment.dto.response.PaymentResponse;
import com.example.payment.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Xử lý một yêu cầu thanh toán mới.
     * Endpoint này sẽ được gọi bởi OrderService.
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            Authentication authentication // (Tùy chọn) nếu cần xác thực request đến PaymentService
                                         // hoặc để lấy userId/thông tin khác từ token
    ) {
        // (Tùy chọn) Kiểm tra xác thực và quyền nếu cần
        // if (authentication == null || !authentication.isAuthenticated()) {
        //     throw new PaymentException(ErrorCodePayment.USER_NOT_AUTHENTICATED_FOR_PAYMENT);
        // }
        // String authenticatedUserId = authentication.getName();
        // if (!authenticatedUserId.equals(paymentRequest.getUserId())) {
        //      throw new PaymentException(ErrorCodePayment.PAYMENT_ACCESS_DENIED, "User ID in request does not match authenticated user.");
        // }


        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
        ApiResponse<PaymentResponse> response = ApiResponse.<PaymentResponse>builder()
                .code(paymentResponse.getStatus() == com.example.payment.entity.PaymentStatus.SUCCESS ||
                      paymentResponse.getStatus() == com.example.payment.entity.PaymentStatus.PENDING && paymentResponse.getRedirectUrl() != null ?
                      HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value()) // Hoặc mã thành công/thất bại tùy chỉnh
                .message(paymentResponse.getMessage())
                .data(paymentResponse)
                .build();

        // Nếu thanh toán thành công hoặc cần redirect, trả về 200 OK.
        // Nếu thanh toán thất bại ngay lập tức (không redirect), có thể trả về 400 Bad Request.
        HttpStatus status = (paymentResponse.getStatus() == com.example.payment.entity.PaymentStatus.SUCCESS ||
                            (paymentResponse.getStatus() == com.example.payment.entity.PaymentStatus.PENDING && paymentResponse.getRedirectUrl() != null)) ?
                           HttpStatus.OK : HttpStatus.BAD_REQUEST;

        return new ResponseEntity<>(response, status);
    }

    /**
     * Lấy thông tin thanh toán theo ID đơn hàng.
     * Endpoint này có thể được gọi bởi OrderService để kiểm tra trạng thái thanh toán.
     */
    @GetMapping("/order/{orderId}")
    // (Tùy chọn) @PreAuthorize("@paymentSecurityService.canAccessPaymentsForOrder(authentication, #orderId) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable Long orderId,
            Authentication authentication // (Tùy chọn)
    ) {
        PaymentResponse paymentResponse = paymentService.getPaymentByOrderId(orderId);
        ApiResponse<PaymentResponse> response = ApiResponse.<PaymentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin thanh toán cho đơn hàng ID " + orderId + " thành công.")
                .data(paymentResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin thanh toán theo ID thanh toán.
     */
    @GetMapping("/{paymentId}")
    // (Tùy chọn) @PreAuthorize("@paymentSecurityService.canAccessPayment(authentication, #paymentId) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable String paymentId,
            Authentication authentication // (Tùy chọn)
    ) {
        PaymentResponse paymentResponse = paymentService.getPaymentById(paymentId);
         ApiResponse<PaymentResponse> response = ApiResponse.<PaymentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin thanh toán ID " + paymentId + " thành công.")
                .data(paymentResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    // (Tùy chọn) Endpoint cho webhook từ cổng thanh toán
    // @PostMapping("/webhook/gateway-callback")
    // public ResponseEntity<Void> handleGatewayWebhook(@RequestBody Map<String, Object> payload, @RequestHeader Map<String, String> headers) {
    //     // Xác thực webhook (ví dụ: kiểm tra signature header)
    //     // Phân tích payload để lấy gatewayTransactionId, status mới
    //     // paymentService.updatePaymentStatusFromWebhook(...);
    //     return ResponseEntity.ok().build();
    // }
}
