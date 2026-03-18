package com.example.minigame.client;

import com.example.minigame.dto.UserVoucherDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "voucher-service", url = "http://voucher-service:8089/voucher/vouchers")
public interface VoucherClient {

    // 🎁 Gọi sang voucher-service để phát voucher cho user
    @PostMapping("/issue/{userId}")
    void issueVoucher(
            @PathVariable("userId") String userId,
            @RequestParam("code") String code
    );

    // 📜 Lấy danh sách voucher của user từ voucher-service
    @GetMapping("/user/{userId}")
    List<UserVoucherDTO> getUserVouchers(@PathVariable("userId") String userId);

    @PostMapping("/redeem-by-points/{userId}")
    UserVoucherDTO redeemVoucherByPoints(@PathVariable("userId") String userId,
                                         @RequestParam("code") String code);
}
