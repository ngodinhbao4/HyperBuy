package com.example.voucher.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "minigame-service",
        url = "${minigame.service.url}" // cấu hình trong application.yml
)
public interface LoyaltyClient {

    @PostMapping("minigame/api/v1/loyalty/spend")
    LoyaltySpendPointsResponse spendPoints(@RequestBody LoyaltySpendPointsRequest request);
}
