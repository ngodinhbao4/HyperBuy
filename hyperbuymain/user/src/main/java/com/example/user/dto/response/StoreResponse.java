package com.example.user.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class StoreResponse {
    private String storeId;
    private String storeName;
    private String businessLicense;
    private String userId;
    private String username;
    private List<Map<String, Object>> products;
}