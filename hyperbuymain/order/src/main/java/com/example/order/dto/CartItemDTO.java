package com.example.order.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank; 
import jakarta.validation.constraints.NotNull; 

@Data 
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItemDTO {

    @NotBlank(message = "Product ID không được để trống hoặc chỉ chứa khoảng trắng.")
    private Long productId;
    
    @NotNull(message = "Số lượng không được để null.")
    @Min(value = 1, message = "Số lượng phải ít nhất là 1.")
    private Integer quantity;

    // (Tùy chọn) Nếu bạn muốn OrderService sử dụng giá tại thời điểm thêm vào giỏ:
    // private java.math.BigDecimal priceAtAddition;
    // Nếu thêm trường này, nhớ cập nhật @AllArgsConstructor hoặc thêm constructor/setter phù hợp.
}
