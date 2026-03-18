package com.example.voucher.service;

import com.example.voucher.dto.VoucherResponse;
import com.example.voucher.entity.UserVoucher;
import com.example.voucher.entity.Voucher;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    Voucher createVoucher(Voucher voucher);
    List<Voucher> getAllVouchers();
    UserVoucher issueVoucherToUser(String userId, String code);
    List<UserVoucher> getUserVouchers(String userId);
       // ✅ THÊM: tính số tiền giảm
    BigDecimal calculateDiscount(String userId, String code, BigDecimal orderAmount);

    // ✅ THÊM: đánh dấu voucher đã dùng (gọi sau khi thanh toán thành công)
    void markVoucherUsed(String userId, String code);

    List<VoucherResponse> getAvailableVouchers(String userId);

    UserVoucher redeemVoucherByPoints(String userId, String code);

}
