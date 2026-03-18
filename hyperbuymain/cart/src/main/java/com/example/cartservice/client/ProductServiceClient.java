package com.example.cartservice.client;

import com.example.cartservice.dto.request.ProductDetailRequest;
import com.example.cartservice.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "product-service", url = "${product.service.url:http://localhost:8081}")
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/{productId}")
    ApiResponse<ProductDetailRequest> getProductById(
            @PathVariable("productId") String productId,
            @RequestHeader("Authorization") String authorization
    );
}
