package com.example.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotBlank(message = "User ID không được để trống")
    private String userId;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Đơn vị tiền tệ không được để trống")
    private String currency; // Ví dụ: "VND"

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // Ví dụ: "CREDIT_CARD", "MOMO", "COD"

    // Chi tiết cụ thể của phương thức thanh toán, ví dụ: thông tin thẻ, token từ client-side SDK
    // Đối với COD, có thể để trống hoặc không cần.
    // Đối với thẻ, không bao giờ truyền trực tiếp số thẻ đầy đủ qua đây nếu không đạt chuẩn PCI DSS.
    // Thường sẽ là một token đại diện cho thông tin thẻ đã được mã hóa bởi client/payment gateway.
    private Map<String, Object> paymentDetails; // Ví dụ: {"cardNumberLast4": "1234", "cardToken": "tok_xyz"}
}