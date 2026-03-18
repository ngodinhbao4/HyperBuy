// Trong order-service/src/main/java/com/yourapp/orderservice/dto/response/ApiResponse.java
package com.example.order.dto.response; // Hoặc package phù hợp của bạn

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Không serialize các trường null
public class ApiResponse<T> {
    private int code; // Mã lỗi tùy chỉnh của bạn
    private String message;
    private T data; // Dữ liệu trả về (nếu có)

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200) // Hoặc mã thành công chung của bạn
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .build();
    }
    // Các builder cho lỗi có thể được tạo trong GlobalExceptionHandler
}