package com.example.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data // Lombok: tự tạo getters, setters, toString, equals, hashCode
public class CategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 255, message = "Category name cannot exceed 255 characters")
    private String name;

    private String description; // Optional
}
