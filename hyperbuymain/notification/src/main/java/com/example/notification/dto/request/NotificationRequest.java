package com.example.notification.dto.request;

import lombok.Data;

@Data
public class NotificationRequest {
    private String userId; // UUID
    private String message; // Nội dung thông báo
}