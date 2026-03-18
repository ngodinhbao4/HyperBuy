package com.example.voucher.controller;

import com.example.voucher.entity.UserVoucher;
import com.example.voucher.entity.Voucher;
import com.example.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // ✅ Admin tạo voucher
    @PostMapping
    public ResponseEntity<Voucher> create(@RequestBody Voucher voucher) {
        return ResponseEntity.ok(voucherService.createVoucher(voucher));
    }

    // ✅ Xem tất cả voucher (cho admin hoặc public)
    @GetMapping
    public ResponseEntity<List<Voucher>> all() {
        return ResponseEntity.ok(voucherService.getAllVouchers());
    }

    // ✅ Phát voucher cho user (gọi từ MiniGame hoặc admin)
    @PostMapping("/issue/{userId}")
    public ResponseEntity<UserVoucher> issue(
            @PathVariable String userId,
            @RequestParam String code
    ) {
        return ResponseEntity.ok(voucherService.issueVoucherToUser(userId, code));
    }

    // ✅ Lấy danh sách voucher của user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserVoucher>> getUserVouchers(@PathVariable String userId) {
        return ResponseEntity.ok(voucherService.getUserVouchers(userId));
    }

    // ✅ Áp dụng voucher cho user, trả về số tiền được giảm
     @GetMapping("/apply")
    public ResponseEntity<BigDecimal> applyVoucher(
            @RequestParam String userId,
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount
    ) {
        BigDecimal discount = voucherService.calculateDiscount(userId, code, orderAmount);
        return ResponseEntity.ok(discount);
    }


    @PostMapping("/use")
    public ResponseEntity<Void> useVoucher(
            @RequestParam String userId,
            @RequestParam String code
    ) {
        voucherService.markVoucherUsed(userId, code);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/available")
    public ResponseEntity<?> getAvailableVouchers(
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(voucherService.getAvailableVouchers(userId));
    }

    // ✅ Đổi điểm để lấy voucher
@PostMapping("/redeem-by-points/{userId}")
public ResponseEntity<UserVoucher> redeemByPoints(
        @PathVariable String userId,
        @RequestParam String code
) {
    UserVoucher userVoucher = voucherService.redeemVoucherByPoints(userId, code);
    return ResponseEntity.ok(userVoucher);
}


}
