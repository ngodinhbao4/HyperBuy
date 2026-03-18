// File: src/main/java/com/example/productservice/controller/dto/UpdateStockRequest.java
package com.example.product.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Lombok: tự động tạo getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: tự động tạo constructor không tham số
@AllArgsConstructor // Lombok: tự động tạo constructor với tất cả các tham số
public class UpdateStockRequest {

    @NotNull(message = "Stock change quantity cannot be null")
    private Integer change; // Số lượng thay đổi: giá trị dương (+) để tăng, giá trị âm (-) để giảm

}