package com.example.notification.dto;

import java.util.List;
import lombok.Data;

@Data
public class UserResponse {
    private String id; // UUID
    private String username;
    private String name;
    private String email;
    private String dob;
    private List<Role> role;

    @Data
    public static class Role {
        private String name;
        private String description;
        private List<String> permission;
    }
}