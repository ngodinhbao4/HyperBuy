// VoucherServiceClient.java
package com.example.order.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(
        name = "voucher-service",
        url = "${voucher.service.url:http://localhost:8089}"
)
public interface VoucherServiceClient {

    // ✅ Tính số tiền giảm
    @GetMapping("/voucher/vouchers/apply")
    BigDecimal applyVoucher(
            @RequestParam("userId") String userId,
            @RequestParam("code") String code,
            @RequestParam("orderAmount") BigDecimal orderAmount,
            @RequestHeader("Authorization") String authorizationHeader
    );

    // ✅ Đánh dấu đã dùng voucher
    @PostMapping("/voucher/vouchers/use")
    void markVoucherUsed(
            @RequestParam("userId") String userId,
            @RequestParam("code") String code,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
