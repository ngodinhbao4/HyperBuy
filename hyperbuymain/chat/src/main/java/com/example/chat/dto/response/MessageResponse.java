package com.example.chat.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private String senderId;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;
}