// Package: com.example.cartservice.exception
package com.example.cartservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import lombok.Getter;

@Getter
public enum ErrorCodeCart {
    // Lỗi chung
    UNCATEGORIZED_CART_EXCEPTION(3001, "Uncategorized cart error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_CART_REQUEST(3002, "Invalid request data for cart operation", HttpStatus.BAD_REQUEST),

    // Lỗi liên quan đến Cart
    CART_NOT_FOUND(3100, "Cart not found for the user", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND_IN_CART(3101, "Không tìm thấy sản phẩm trong giỏ", HttpStatus.NOT_FOUND),

    // Lỗi liên quan đến Product (khi tương tác với ProductService hoặc kiểm tra sản phẩm)
    PRODUCT_NOT_AVAILABLE(2100, "Sản phẩm không tồn tại", HttpStatus.BAD_REQUEST), // Ví dụ: hết hàng, không tồn tại
    INSUFFICIENT_STOCK(2101, "Sản phẩm không đủ hàng tồn kho", HttpStatus.BAD_REQUEST),
    PRODUCT_SERVICE_UNREACHABLE(2102, "Product service is unreachable or returned an error", HttpStatus.SERVICE_UNAVAILABLE),

    // Lỗi liên quan đến User (khi tương tác với UserService)
    USER_NOT_FOUND_FOR_CART(1100, "User not found, cannot perform cart operation", HttpStatus.NOT_FOUND),
    USER_SERVICE_UNREACHABLE(1101, "User service is unreachable", HttpStatus.SERVICE_UNAVAILABLE),
    USER_NOT_AUTHENTICATED(1102, "User not authenticated. Access token is missing, invalid, or expired.", HttpStatus.UNAUTHORIZED),


    // Lỗi xác thực/ủy quyền cho giỏ hàng (nếu có logic đặc biệt)
    CART_ACCESS_DENIED(3300, "Access to this cart is denied", HttpStatus.FORBIDDEN)
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCodeCart(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}