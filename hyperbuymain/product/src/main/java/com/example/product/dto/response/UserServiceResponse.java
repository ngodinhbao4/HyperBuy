package com.example.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserServiceResponse {
    private int code;
    private UserResult result;

    // Getters và setters
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class UserResult {
        private String storeId;
        private String storeName;
        private String userId;
        private String username;
        // Getters và setters
    }
}
