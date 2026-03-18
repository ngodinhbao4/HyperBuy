package com.example.minigame.controller;



import com.example.minigame.dto.reponse.SpendPointsResponse;
import com.example.minigame.dto.request.SpendPointsRequest;
import com.example.minigame.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    // ✅ Voucher-service sẽ gọi API này để trừ điểm
    @PostMapping("/spend")
    public ResponseEntity<SpendPointsResponse> spendPoints(@RequestBody SpendPointsRequest request) {
        SpendPointsResponse response = loyaltyService.spendPoints(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

}
