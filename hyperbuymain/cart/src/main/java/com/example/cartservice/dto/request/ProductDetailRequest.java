package com.example.cartservice.dto.request;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDetailRequest {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private Integer stockQuantity;
}
