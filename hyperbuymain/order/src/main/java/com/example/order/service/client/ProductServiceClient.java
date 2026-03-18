package com.example.order.service.client;

import com.example.order.dto.CategoryDTO;
import com.example.order.dto.ProductDTO;
import com.example.order.dto.response.ApiResponRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "product-service", url = "http://productservice:8081")
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/{productId}")
    ApiResponRequest<ProductDTO> getProductById(
            @PathVariable Long productId,
            @RequestHeader("Authorization") String authorization
    );

    @PutMapping("/api/v1/products/{productId}/stock")
    void decreaseStock(
            @PathVariable Long productId,
            @RequestBody UpdateStockRequest stockRequest,
            @RequestHeader("Authorization") String authorization
    );

    @PutMapping("/api/v1/products/{productId}/stock/increase")
    void increaseStock(
            @PathVariable Long productId,
            @RequestBody UpdateStockRequest stockRequest,
            @RequestHeader("Authorization") String authorization
    );

    @GetMapping("/api/v1/products/{productId}/category")
    CategoryDTO getCategoryByProductId(
            @PathVariable Long productId,
            @RequestHeader("Authorization") String token
    );
}
