package com.example.order.controller;

import com.example.order.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderQueryService orderQueryService;

    // GET /api/v1/internal/orders/has-purchased?username=baodh&productId=1
    @GetMapping("/has-purchased")
    public ResponseEntity<Boolean> hasPurchased(
            @RequestParam String username,
            @RequestParam Long productId
    ) {
        boolean purchased = orderQueryService.hasUserPurchasedProduct(username, productId);
        return ResponseEntity.ok(purchased);
    }
}
