package com.example.user.dto.request;

import lombok.Data;

@Data
public class SellerRequest {
    private String storeName; // Tên cửa hàng (tùy chọn)
    private String businessLicense; // Giấy phép kinh doanh (tùy chọn)
}