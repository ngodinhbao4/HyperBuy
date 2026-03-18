package com.example.order.dto.request;

import java.util.List;

import com.example.order.dto.AddressDTO;
import com.example.order.dto.CartItemDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    private String userId; // hiện đang truyền username (vd: admin, baodh)

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressDTO shippingAddress;

    @Valid
    private AddressDTO billingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // "CREDIT_CARD", "COD", ...

    // ✅ Mã voucher (tùy chọn)
    private String voucherCode;

    // (nếu sau này bạn cho phép client gửi items, vẫn giữ)
    private List<CartItemDTO> items;
}
