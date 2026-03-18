
package com.example.payment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCodePayment {
    // Lỗi chung của PaymentService (ví dụ: bắt đầu từ 5000 để phân biệt với các service khác)
    UNCATEGORIZED_PAYMENT_EXCEPTION(5001, "Lỗi thanh toán không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_PAYMENT_REQUEST(5002, "Dữ liệu yêu cầu thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    PAYMENT_VALIDATION_FAILED(5003, "Xác thực dữ liệu thanh toán thất bại", HttpStatus.BAD_REQUEST),

    // Lỗi liên quan đến nghiệp vụ Thanh toán
    PAYMENT_NOT_FOUND(5100, "Không tìm thấy thông tin thanh toán", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND_FOR_ORDER(5101, "Không tìm thấy thanh toán cho đơn hàng này", HttpStatus.NOT_FOUND),
    PAYMENT_PROCESSING_FAILED(5102, "Xử lý thanh toán thất bại do lỗi nội bộ", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_GATEWAY_ERROR(5103, "Lỗi từ cổng thanh toán", HttpStatus.BAD_GATEWAY), // Khi cổng TT trả lỗi không mong muốn
    PAYMENT_DECLINED_BY_GATEWAY(5104, "Thanh toán bị từ chối bởi cổng thanh toán", HttpStatus.BAD_REQUEST), // Khi cổng TT từ chối hợp lệ
    PAYMENT_ALREADY_PROCESSED(5105, "Thanh toán đã được xử lý hoặc đang chờ", HttpStatus.CONFLICT),
    REFUND_FAILED(5106, "Xử lý hoàn tiền thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_PAYMENT_STATUS_FOR_OPERATION(5107, "Trạng thái thanh toán không hợp lệ cho hành động này", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_FUNDS(5108, "Không đủ số dư để thực hiện thanh toán", HttpStatus.BAD_REQUEST), // Ví dụ lỗi cụ thể từ cổng TT

    // Lỗi giao tiếp với các service khác (nếu PaymentService cần gọi service khác)
    ORDER_SERVICE_UNREACHABLE_FOR_PAYMENT(5300, "Không thể kết nối đến Order Service trong quá trình xử lý thanh toán", HttpStatus.SERVICE_UNAVAILABLE),
    NOTIFICATION_SERVICE_UNREACHABLE_FOR_PAYMENT(5301, "Không thể kết nối đến Notification Service sau khi xử lý thanh toán", HttpStatus.SERVICE_UNAVAILABLE),

    // Lỗi xác thực/ủy quyền cho PaymentService
    PAYMENT_ACCESS_DENIED(5400, "Truy cập vào thông tin thanh toán hoặc hành động thanh toán bị từ chối", HttpStatus.FORBIDDEN),
    USER_NOT_AUTHENTICATED_FOR_PAYMENT(5401, "Người dùng chưa được xác thực để thực hiện hành động liên quan đến thanh toán.", HttpStatus.UNAUTHORIZED),
    INVALID_WEBHOOK_SIGNATURE(5402, "Chữ ký webhook từ cổng thanh toán không hợp lệ", HttpStatus.UNAUTHORIZED), // Nếu có webhook
    SERVICE_TO_SERVICE_AUTH_FAILED(5403, "Xác thực giữa các dịch vụ thất bại khi gọi PaymentService", HttpStatus.UNAUTHORIZED) // Nếu có cơ chế bảo vệ S2S
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCodePayment(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
