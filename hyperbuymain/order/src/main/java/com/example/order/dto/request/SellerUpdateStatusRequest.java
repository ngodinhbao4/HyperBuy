package com.example.order.dto.request;

import lombok.Data;

@Data
public class SellerUpdateStatusRequest {
    private String storeId;    // bắt buộc để seller không update nhầm của người khác
    private String status;     // PROCESSING / SHIPPING / DELIVERED
}
