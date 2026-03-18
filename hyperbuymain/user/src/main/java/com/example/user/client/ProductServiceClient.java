package com.example.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service", url = "${product.service.url}")
public interface ProductServiceClient {
    @GetMapping("/api/v1/products/store/{storeId}")
    List<Map<String, Object>> getProductsByStoreId(
            @PathVariable("storeId") String storeId,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}