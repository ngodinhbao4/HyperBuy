package com.example.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SellerRequestResponse {
    private String id;
    private String userId;
    private String username;
    private String storeName;
    private String businessLicense;
    private String status;
}