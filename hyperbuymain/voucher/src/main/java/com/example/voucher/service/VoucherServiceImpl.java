package com.example.voucher.service;

import com.example.voucher.dto.VoucherResponse;
import com.example.voucher.entity.UserVoucher;
import com.example.voucher.entity.Voucher;
import com.example.voucher.repository.UserVoucherRepository;
import com.example.voucher.repository.VoucherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.voucher.client.LoyaltyClient;
import com.example.voucher.client.LoyaltySpendPointsRequest;
import com.example.voucher.client.LoyaltySpendPointsResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final LoyaltyClient loyaltyClient;

    @Override
    public Voucher createVoucher(Voucher voucher) {
        return voucherRepository.save(voucher);
    }

    @Override
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @Override
    public UserVoucher issueVoucherToUser(String userId, String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        UserVoucher uv = UserVoucher.builder()
                .userId(userId)
                .voucher(voucher)
                .build();
        return userVoucherRepository.save(uv);
    }

    @Override
    public List<UserVoucher> getUserVouchers(String userId) {
        return userVoucherRepository.findByUserId(userId);
    }

    // ✅ TÍNH GIẢM GIÁ KHI THANH TOÁN
    @Override
    public BigDecimal calculateDiscount(String userId, String code, BigDecimal orderAmount) {
        // Mặc định không giảm
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        UserVoucher userVoucher = userVoucherRepository
            .findFirstByUserIdAndVoucher_CodeAndUsedFalseOrderByIdAsc(userId, code)
            .orElse(null);

        if (userVoucher == null) {
            // user không có voucher này hoặc đã dùng
            return BigDecimal.ZERO;
        }

        Voucher voucher = userVoucher.getVoucher();

        // Kiểm tra trạng thái
        if (voucher.getStatus() != Voucher.Status.ACTIVE) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra thời gian hiệu lực
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return BigDecimal.ZERO;
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra số lượng còn lại
        Integer quantity = voucher.getQuantity() != null ? voucher.getQuantity() : 0;
        Integer used = voucher.getUsed() != null ? voucher.getUsed() : 0;
        if (quantity > 0 && used >= quantity) {
            return BigDecimal.ZERO; // hết lượt
        }

        // Tính số tiền giảm
        Double discountValue = voucher.getDiscountValue();
        if (discountValue == null || discountValue <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            // giảm theo %
            BigDecimal percent = BigDecimal.valueOf(discountValue)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = orderAmount.multiply(percent);
        } else if ("AMOUNT".equalsIgnoreCase(voucher.getDiscountType())) {
            // giảm số tiền cố định
            discount = BigDecimal.valueOf(discountValue);
        } else {
            // loại không hợp lệ
            return BigDecimal.ZERO;
        }

        // Không được giảm quá tổng đơn
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        // Làm tròn 2 chữ số thập phân
        discount = discount.setScale(2, RoundingMode.HALF_UP);

        return discount;
    }

    // ✅ GỌI SAU KHI ORDER ĐÃ THANH TOÁN THÀNH CÔNG
    @Override
    public void markVoucherUsed(String userId, String code) {
        UserVoucher userVoucher = userVoucherRepository
                .findFirstByUserIdAndVoucher_CodeAndUsedFalseOrderByIdAsc(userId, code)
                .orElse(null);

        if (userVoucher == null) {
            return; // không có voucher unused thì thôi
        }
        Voucher voucher = userVoucher.getVoucher();

        // đánh dấu userVoucher
        userVoucher.setUsed(true);
        userVoucherRepository.save(userVoucher);

        // tăng used của voucher
        Integer used = voucher.getUsed() != null ? voucher.getUsed() : 0;
        voucher.setUsed(used + 1);

        // nếu đã dùng >= quantity => có thể set INACTIVE
        Integer quantity = voucher.getQuantity() != null ? voucher.getQuantity() : 0;
        if (quantity > 0 && voucher.getUsed() >= quantity) {
            voucher.setStatus(Voucher.Status.INACTIVE);
        }

        voucherRepository.save(voucher);
    }

    public List<VoucherResponse> getAvailableVouchers(String userId) {
    LocalDateTime now = LocalDateTime.now();

    List<UserVoucher> userVouchers = userVoucherRepository.findByUserIdAndUsedFalse(userId);

    return userVouchers.stream()
            .map(UserVoucher::getVoucher)
            .filter(v -> 
                    // voucher đã bắt đầu (startDate <= now hoặc null thì bỏ qua)
                    (v.getStartDate() == null || !v.getStartDate().isAfter(now)) &&
                    // voucher chưa hết hạn (endDate >= now hoặc null thì bỏ qua)
                    (v.getEndDate() == null || !v.getEndDate().isBefore(now)) &&
                    // chỉ lấy voucher đang ACTIVE
                    v.getStatus() == Voucher.Status.ACTIVE
            )
            .map(v -> new VoucherResponse(
        v.getId(),
        v.getCode(),
        v.getDiscountType() != null ? v.getDiscountType().toString() : null,
        v.getDiscountValue(),
        v.getStartDate(),
        v.getEndDate(),
        v.getQuantity(),
        v.getStatus() != null ? v.getStatus().toString() : null,
        v.getPointCost()   // 🔥 THÊM DÒNG NÀY
))
            .collect(Collectors.toList());
}

    @Override
    @Transactional
    public UserVoucher redeemVoucherByPoints(String userId, String code) {
        // 1️⃣ Lấy voucher theo code
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        // 2️⃣ Kiểm tra trạng thái & thời gian, quantity giống calculateDiscount
        LocalDateTime now = LocalDateTime.now();

        if (voucher.getStatus() != Voucher.Status.ACTIVE) {
            throw new RuntimeException("Voucher không còn hiệu lực");
        }

        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new RuntimeException("Voucher chưa bắt đầu áp dụng");
        }

        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new RuntimeException("Voucher đã hết hạn");
        }

        Integer quantity = voucher.getQuantity() != null ? voucher.getQuantity() : 0;
        Integer used = voucher.getUsed() != null ? voucher.getUsed() : 0;
        if (quantity > 0 && used >= quantity) {
            throw new RuntimeException("Voucher đã hết lượt phát hành");
        }

        // 3️⃣ Kiểm tra pointCost
        Integer pointCost = voucher.getPointCost();
        if (pointCost == null || pointCost <= 0) {
            throw new RuntimeException("Voucher này không hỗ trợ đổi bằng điểm");
        }

        // 4️⃣ Gọi minigame-service để TRỪ ĐIỂM
        LoyaltySpendPointsRequest req = new LoyaltySpendPointsRequest(
                userId,
                pointCost,
                "REDEEM_VOUCHER",
                code
        );

        LoyaltySpendPointsResponse resp = loyaltyClient.spendPoints(req);

        if (resp == null || !resp.isSuccess()) {
            throw new RuntimeException(resp != null ? resp.getMessage() : "Không thể trừ điểm từ loyalty-service");
        }

        // 5️⃣ Nếu trừ điểm thành công => gán voucher cho user
        UserVoucher userVoucher = UserVoucher.builder()
                .userId(userId)
                .voucher(voucher)
                .build();
        userVoucher = userVoucherRepository.save(userVoucher);

        // 6️⃣ Tăng used của voucher
        voucher.setUsed((voucher.getUsed() != null ? voucher.getUsed() : 0) + 1);
        if (quantity > 0 && voucher.getUsed() >= quantity) {
            voucher.setStatus(Voucher.Status.INACTIVE);
        }
        voucherRepository.save(voucher);

        return userVoucher;
    }

}
