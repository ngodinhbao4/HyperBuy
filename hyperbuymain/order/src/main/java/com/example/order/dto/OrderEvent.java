package com.example.order.dto;

import lombok.Data;

@Data
public class OrderEvent {
    private Long id;
    private String userId;
    private String status;
    private String authorizationHeader;
}