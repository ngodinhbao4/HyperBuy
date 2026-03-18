// Trong order-service/src/main/java/com/yourapp/orderservice/dto/CartDTO.java
package com.example.order.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank; // Sử dụng NotBlank cho String
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartDTO {

    @NotBlank(message = "User ID from cart cannot be null or empty") // Thay đổi từ Long sang String
    private String userId;

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid // Để validate các CartItemDTO bên trong
    private List<CartItemDTO> items;
}