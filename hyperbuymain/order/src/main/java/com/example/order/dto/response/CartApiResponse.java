package com.example.order.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartApiResponse<T> {
    private Integer code;
    private String message;
    private T result; // Phù hợp với phản hồi từ cart-service
}