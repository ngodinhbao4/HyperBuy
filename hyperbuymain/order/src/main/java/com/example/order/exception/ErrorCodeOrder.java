// Trong order-service/src/main/java/com/yourapp/orderservice/exception/ErrorCodeOrder.java
package com.example.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCodeOrder {
    // Lỗi chung OrderService (ví dụ: 40xx)
    UNCATEGORIZED_ORDER_EXCEPTION(4001, "Uncategorized order error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ORDER_REQUEST(4002, "Dữ liệu yêu cầu không hợp lệ cho hoạt động đặt hàng", HttpStatus.BAD_REQUEST),
    ORDER_VALIDATION_FAILED(4003, "Xác thực đặt hàng không thành công", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(4004,"Không thấy user", HttpStatus.BAD_REQUEST),

    // Lỗi liên quan đến nghiệp vụ Order (ví dụ: 41xx)
    ORDER_NOT_FOUND(4100, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    EMPTY_CART_FOR_ORDER(4101, "Giỏ Hàng trống, Không thể tạo đơn hàng", HttpStatus.BAD_REQUEST),
    ORDER_CREATION_FAILED(4102, "Failed to create order due to an internal issue", HttpStatus.INTERNAL_SERVER_ERROR),
    ORDER_UPDATE_FAILED(4103, "Failed to update order status", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ORDER_STATUS_TRANSITION(4104, "Invalid order status transition", HttpStatus.BAD_REQUEST),
    ORDER_CANCEL_NOT_ALLOWED(4105, "Order cancellation is not allowed for its current status", HttpStatus.BAD_REQUEST),
    DATABASE_ERROR(4106, "Database error occurred", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(4107, "Invalid or expired token",HttpStatus.BAD_REQUEST),

    // Lỗi liên quan đến Product khi tạo Order (ví dụ: 42xx - có thể trùng với cart nhưng message cụ thể hơn cho order)
    // Các mã lỗi này có thể tham chiếu hoặc tương tự ErrorCodeCart nhưng nên được định nghĩa riêng
    // để OrderService không phụ thuộc trực tiếp vào ErrorCode của service khác.
    PRODUCT_NOT_FOUND_FOR_ORDER(4200, "Không tìm thấy sản phẩm để lập hóa đơn", HttpStatus.NOT_FOUND),
    PRODUCT_UNAVAILABLE_FOR_ORDER(4201, "Sản phẩm không có sẵn để đặt hàng", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK_FOR_ORDER(4202, "Không đủ hàng tồn kho cho sản phẩm trong quá trình tạo đơn hàng", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_MISMATCH(4203, "Giá sản phẩm đã thay đổi, vui lòng xem lại đơn hàng của bạn", HttpStatus.CONFLICT), // Ví dụ

    // Lỗi khi giao tiếp với các service khác (ví dụ: 43xx)
    CART_SERVICE_UNREACHABLE(4300, "Cart service is unreachable or returned an error", HttpStatus.SERVICE_UNAVAILABLE),
    PRODUCT_SERVICE_UNREACHABLE_FOR_ORDER(4301, "Product service is unreachable or returned an error during order processing", HttpStatus.SERVICE_UNAVAILABLE),
    PAYMENT_SERVICE_ERROR(4302, "Error occurred with the payment service", HttpStatus.INTERNAL_SERVER_ERROR), // Nếu có PaymentService
    NOTIFICATION_SERVICE_ERROR(4303, "Failed to send order notification", HttpStatus.INTERNAL_SERVER_ERROR), // Nếu có NotificationService
    USER_SERVICE_UNREACHABLE_FOR_ORDER(4304, "User service is unreachable or returned an error", HttpStatus.SERVICE_UNAVAILABLE),
    PRODUCT_SERVICE_STOCK_UPDATE_FAILED(4305, "Failed to update stock via Product Service", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(4306, "Insufficient stock", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_AVAILABLE(4307, "Product not available", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_UPDATE(5001, "Không thể cập nhật trạng thái này",HttpStatus.BAD_REQUEST),
    FORBIDDEN_SELLER_ORDER(5002, "Đơn hàng không thuộc quyền xử lý của seller",HttpStatus.BAD_REQUEST),
    USER_SERVICE_UNREACHABLE(4308, "Dịch vụ người dùng không thể truy cập hoặc đã trả về lỗi trong quá trình xử lý đơn hàng", HttpStatus.SERVICE_UNAVAILABLE),
    STORE_ID_NULL(4309, "Mã cửa hàng rỗng", HttpStatus.BAD_REQUEST),

    // Lỗi xác thực/ủy quyền (ví dụ: 44xx)
    ORDER_ACCESS_DENIED(4400, "Access to this order is denied", HttpStatus.FORBIDDEN),
    USER_NOT_AUTHENTICATED_FOR_ORDER(4401, "User not authenticated for order operation.", HttpStatus.UNAUTHORIZED)
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCodeOrder(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}