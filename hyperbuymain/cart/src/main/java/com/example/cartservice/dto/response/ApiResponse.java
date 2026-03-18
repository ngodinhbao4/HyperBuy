// Package: com.example.cartservice.dto.response (hoặc một package chung)
package com.example.cartservice.dto.response; // Hoặc một package dùng chung

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Không bao gồm các trường null trong JSON response
public class ApiResponse<T> { // Đổi tên thành ApiResponseDto hoặc tương tự nếu thích
    private int code;
    private String message;
    private T result; // Thêm trường result nếu bạn muốn trả về dữ liệu cụ thể
}