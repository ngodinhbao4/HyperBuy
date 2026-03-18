package com.example.product.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "http://orderservice:8083")
public interface OrderClient {

    @GetMapping("/api/v1/internal/orders/has-purchased")
    Boolean hasPurchased(
            @RequestParam("username") String username,
            @RequestParam("productId") Long productId
    );
}
