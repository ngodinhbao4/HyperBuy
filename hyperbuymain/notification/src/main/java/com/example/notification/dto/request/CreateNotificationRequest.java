package com.example.notification.dto.request;

import lombok.Data;

@Data
public class CreateNotificationRequest {
    private String userId; // ID của user nhận thông báo
    private String message; // Nội dung thông báo
}