package com.example.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "Tên đăng nhập đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Tên đăng nhập phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Mật khẩu phải có ít nhất 8 ký tự", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "Không tìm thấy username", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "Bạn không có quyền", HttpStatus.FORBIDDEN),
    ROLE_NOT_FOUND(1008, "Vai trò không tồn tại", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_STATUS(1009,"Invalid request status", HttpStatus.BAD_REQUEST),
    REQUEST_NOT_FOUND(1010,"Request not found", HttpStatus.BAD_REQUEST),
    PENDING_REQUEST_EXISTS(1011, "Bạn đã có một yêu cầu đang chờ xử lý", HttpStatus.BAD_REQUEST),
    ACCOUNT_BANNED(1012, "Tài khoản của bạn đã bị khóa", HttpStatus.BAD_REQUEST),
    SELLER_PROFILE_NOT_FOUND(1013,"Không tìm thấy thông tin người bán", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1014,"Sai Mật khẩu", HttpStatus.BAD_REQUEST)
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
